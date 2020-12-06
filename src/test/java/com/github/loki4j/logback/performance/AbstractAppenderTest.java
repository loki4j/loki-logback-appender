package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.List;

import com.github.loki4j.logback.performance.Benchmarker.Benchmark;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class AbstractAppenderTest {

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = withAppender(dummyAppender(capacity, 60_000L, defaultToStringEncoder()), a -> {
            return withAppender(dummyAppender(capacity, 60_000L, defaultToStringEncoder()), aw -> {
                try {
                    return Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
                        this.runs = 100;
                        this.parFactor = 1;
                        this.generator = () -> generateEvents(1_000_000, 10);
                        this.benchmarks = List.of(
                            Benchmark.of("dummyAppender", e -> a.append(e)),
                            Benchmark.of("dummyAppenderWait", e -> aw.appendAndWait(e))
                        );
                    }});
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
        stats.forEach(System.out::println);
    }

    @Test
    @Category({PerformanceTests.class})
    public void multiThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = withAppender(dummyAppender(capacity, 60_000L, defaultToStringEncoder()), a -> {
            return withAppender(dummyAppender(capacity, 60_000L, defaultToStringEncoder()), aw -> {
                try {
                    return Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
                        this.runs = 100;
                        this.parFactor = 4;
                        this.generator = () -> generateEvents(1_000_000, 10);
                        this.benchmarks = List.of(
                            Benchmark.of("dummyAppender", e -> a.append(e)),
                            Benchmark.of("dummyAppenderWait", e -> aw.appendAndWait(e))
                        );
                    }});
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
        stats.forEach(System.out::println);
    }
    
}
