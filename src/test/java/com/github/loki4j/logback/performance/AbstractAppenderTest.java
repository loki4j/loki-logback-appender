package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.List;

import com.github.loki4j.logback.AbstractLoki4jAppender;
import com.github.loki4j.logback.performance.Benchmarker.Benchmark;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class AbstractAppenderTest {

    private static AppenderWrapper<AbstractLoki4jAppender> initApp(AbstractLoki4jAppender a) {
        a.setVerbose(false);
        a.start();
        return new AppenderWrapper<>(a);
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
                    () -> initApp(dummyAppender(capacity, 60_000L, defaultToStringEncoder())),
                    (a, e) -> a.append(e),
                    a -> a.stop()),
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, defaultToStringEncoder())),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop()),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, jsonEncoder(false, "singleThreadPerformance"))),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop()),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, protobufEncoder(false, "singleThreadPerformance"))),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop())
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
                    () -> initApp(dummyAppender(capacity, 60_000L, defaultToStringEncoder())),
                    (a, e) -> a.append(e),
                    a -> a.stop()),
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, defaultToStringEncoder())),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop()),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, jsonEncoder(false, "singleThreadPerformance"))),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop()),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(dummyAppender(capacity, 60_000L, protobufEncoder(false, "singleThreadPerformance"))),
                    (a, e) -> a.appendAndWait(e),
                    a -> a.stop())
            );
        }});
        stats.forEach(System.out::println);
    }

}
