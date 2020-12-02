package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

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
    public void singleThreadPerformance() {
        var capacity = 1000;

        var cbb = new ConcurrentBatchBuffer<ILoggingEvent, LogRecord>(capacity, LogRecord::create, (e, r) -> eventToRecord(e, r));
        var emptyArray = new LogRecord[0];

        var abq = new ArrayBlockingQueue<LogRecord>(capacity);

        var stats = Benchmarker.run(new Benchmarker.Config() {{
            this.events = 1_000_000;
            this.runs = 10;
            this.benchmarks = new Benchmark[] {
                Benchmark.of("cbb", e -> cbb.add(e, emptyArray)),
                Benchmark.of("abq", e -> {
                    abq.add(eventToRecord(e, LogRecord.create()));
                    if (abq.size() == capacity) {
                        for (int i = 0; i < capacity; i++) {
                            try {
								abq.take();
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
                        }    
                    }
                })
            };
        }});

        stats.forEach(System.out::println);
    }
}
