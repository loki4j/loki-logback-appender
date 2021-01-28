package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.logback.Loki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class AppenderTest {

    private static AppenderWrapper initApp(int capacity, Loki4jEncoder e) {
        var a = appender(capacity, 60_000L, e, dummySender());
        a.setSendQueueSize(200_000);
        a.setVerbose(false);
        a.start();
        return new AppenderWrapper(a);
    }

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("dummyAppender",
                    () -> initApp(capacity, defaultToStringEncoder()),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(capacity, defaultToStringEncoder()),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(capacity, jsonEncoder(false, "singleThreadPerformance")),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(capacity, protobufEncoder(false, "singleThreadPerformance")),
                    (a, e) -> a.append(e),
                    a -> a.stop(true))
            );
        }});

        stats.forEach(System.out::println);
    }

    @Test
    @Category({PerformanceTests.class})
    public void multiThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 2;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("dummyAppender",
                    () -> initApp(capacity, defaultToStringEncoder()),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(capacity, defaultToStringEncoder()),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(capacity, jsonEncoder(false, "singleThreadPerformance")),
                    (a, e) -> a.append(e),
                    a -> a.stop(true)),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(capacity, protobufEncoder(false, "singleThreadPerformance")),
                    (a, e) -> a.append(e),
                    a -> a.stop(true))
            );
        }});
        stats.forEach(System.out::println);
    }

}
