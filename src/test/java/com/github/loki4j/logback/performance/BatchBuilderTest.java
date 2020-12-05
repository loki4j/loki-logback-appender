package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import com.github.loki4j.common.ConcurrentBatchBuffer;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.performance.Benchmarker.Benchmark;

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

        var cbb = new ConcurrentBatchBuffer<ILoggingEvent, LogRecord>(capacity, LogRecord::create, (e, r) -> eventToRecord(e, r));
        var emptyArray = new LogRecord[0];

        var abq = new ArrayBlockingQueue<LogRecord>(capacity);

        var sal = new ArrayList<LogRecord>(capacity);

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> generateEvents(1_000_000, 10);
            this.benchmarks = List.of(
                Benchmark.of("cbb", e -> cbb.add(e, emptyArray)),
                Benchmark.of("abq", e -> {
                    abq.add(eventToRecord(e, LogRecord.create()));
                    if (abq.size() == capacity)
                        abq.clear();
                }),
                Benchmark.of("sal", e -> {
                    sal.add(eventToRecord(e, LogRecord.create()));
                    if (sal.size() == capacity)
                        sal.clear();
                })
            );
        }});

        stats.forEach(System.out::println);
    }

    @Test
    @Category({PerformanceTests.class})
    public void multiThreadPerformance() throws Exception {
        var capacity = 1000;

        var cbb = new ConcurrentBatchBuffer<ILoggingEvent, LogRecord>(capacity, LogRecord::create, (e, r) -> eventToRecord(e, r));
        var emptyArray = new LogRecord[0];

        var abq = new ArrayBlockingQueue<LogRecord>(capacity);

        var sal = Collections.synchronizedList(new ArrayList<LogRecord>(capacity));

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 4;
            this.generator = () -> generateEvents(1_000_000, 10);
            this.benchmarks = List.of(
                Benchmark.of("cbb", e -> cbb.add(e, emptyArray)),
                Benchmark.of("abq", e -> {
                    if (!abq.offer(eventToRecord(e, LogRecord.create())))
                        abq.clear();
                }),
                Benchmark.of("sal", e -> {
                    sal.add(eventToRecord(e, LogRecord.create()));
                    if (sal.size() > capacity)
                        sal.clear();
                })
            );
        }});

        stats.forEach(System.out::println);
    }
}
