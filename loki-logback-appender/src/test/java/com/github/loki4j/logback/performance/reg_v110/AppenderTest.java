package com.github.loki4j.logback.performance.reg_v110;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class AppenderTest {

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("oldAppenderWait",
                    () -> {
                        var a = new Loki4jAppenderV100();
                        a.setContext(new LoggerContext());
                        a.setBatchSize(capacity);
                        a.setBatchTimeoutMs(60_000);
                        a.setFormat(defaultToStringEncoder());
                        a.setHttp(dummySender());
                        a.setVerbose(false);
                        a.start();
                        return new Loki4jAppenderV100.Wrapper<>(a);
                    },
                    (a, e) -> a.appendAndWait(e),
                    a -> {},
                    a -> a.stop()),
                Benchmark.of("newAppenderWait",
                    () -> {
                        var a = appender(capacity, 60_000L, defaultToStringEncoder(), dummySender());
                        a.setVerbose(false);
                        a.start();
                        return new AppenderWrapper(a);
                    },
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("oldInstrumentedAppenderWait",
                    () -> {
                        var a = new InstrumentedLoki4jAppender();
                        a.setContext(new LoggerContext());
                        a.setBatchTimeoutMs(60_000);
                        a.setFormat(defaultToStringEncoder());
                        a.setHttp(dummySender());
                        a.setVerbose(false);
                        a.start();
                        return new Loki4jAppenderV100.Wrapper<>(a);
                    },
                    (a, e) -> a.appendAndWait(e),
                    a -> {},
                    a -> a.stop()),
                Benchmark.of("newMetricsEnabledAppenderWait",
                    () -> {
                        var a = appender(capacity, 60_000L, defaultToStringEncoder(), dummySender());
                        a.setVerbose(false);
                        a.setMetricsEnabled(true);
                        a.start();
                        return new AppenderWrapper(a);
                    },
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop())
            );
        }});

        stats.forEach(System.out::println);
    }
    
}
