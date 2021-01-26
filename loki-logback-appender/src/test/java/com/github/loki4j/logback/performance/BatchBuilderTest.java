package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.loki4j.common.ConcurrentBatchBuffer;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class BatchBuilderTest {

    public static LogRecord eventToRecord(ILoggingEvent e, LogRecord r) {
        r.timestampMs = e.getTimeStamp();
        r.nanos = 0;
        r.stream = "test=dlkjafh";
        r.streamHashCode = r.stream.hashCode();
        r.message = e.getMessage();
        return r;
    }

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var capacity = 1000;
        var emptyArray = new LogRecord[0];
        var clqCounter = new AtomicInteger(0);

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(1_000_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("cbb",
                    () -> new ConcurrentBatchBuffer<ILoggingEvent, LogRecord>(capacity, LogRecord::create, (e, r) -> eventToRecord(e, r)),
                    (r, e) -> r.add(e, emptyArray)),
                Benchmark.of("abq",
                    () -> new ArrayBlockingQueue<LogRecord>(capacity),
                    (r, e) -> {
                        if (!r.offer(eventToRecord(e, LogRecord.create())))
                            r.clear();
                    }),
                Benchmark.of("clq",
                    () -> new ConcurrentLinkedQueue<LogRecord>(),
                    (r, e) -> {
                        r.offer(eventToRecord(e, LogRecord.create()));
                        if (clqCounter.incrementAndGet() > capacity)
                            r.clear();
                    }),
                Benchmark.of("sal",
                    () -> new ArrayList<LogRecord>(capacity),
                    (r, e) -> {
                        r.add(eventToRecord(e, LogRecord.create()));
                        if (r.size() > capacity)
                            r.clear();
                    })
            );
        }});

        stats.forEach(System.out::println);
    }

    @Test
    @Category({PerformanceTests.class})
    public void multiThreadPerformance() throws Exception {
        var capacity = 1000;
        var emptyArray = new LogRecord[0];
        var clqCounter = new AtomicInteger(0);

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 2;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(1_000_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("cbb",
                    () -> new ConcurrentBatchBuffer<ILoggingEvent, LogRecord>(capacity, LogRecord::create, (e, r) -> eventToRecord(e, r)),
                    (r, e) -> r.add(e, emptyArray)),
                Benchmark.of("abq",
                    () -> new ArrayBlockingQueue<LogRecord>(capacity),
                    (r, e) -> {
                        if (!r.offer(eventToRecord(e, LogRecord.create())))
                            r.clear();
                    }),
                Benchmark.of("clq",
                    () -> new ConcurrentLinkedQueue<LogRecord>(),
                    (r, e) -> {
                        r.offer(eventToRecord(e, LogRecord.create()));
                        if (clqCounter.incrementAndGet() > capacity)
                            r.clear();
                    }),
                Benchmark.of("sal",
                    () -> Collections.synchronizedList(new ArrayList<LogRecord>(capacity)),
                    (r, e) -> {
                        r.add(eventToRecord(e, LogRecord.create()));
                        if (r.size() > capacity)
                            r.clear();
                    })
            );
        }});

        stats.forEach(System.out::println);
    }

}
