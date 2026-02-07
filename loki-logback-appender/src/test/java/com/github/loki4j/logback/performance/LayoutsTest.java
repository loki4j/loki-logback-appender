package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.generateEvents;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.github.loki4j.logback.JsonLayout;
import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;

import net.logstash.logback.layout.LogstashLayout;

public class LayoutsTest {

    private static Layout<ILoggingEvent> initLayout(Layout<ILoggingEvent> l) {
        l.setContext(new LoggerContext());
        l.start();
        return l;
    }

    @Test
    @Tag("com.github.loki4j.testkit.categories.PerformanceTests")
    public void layoutPerformance() throws Exception {
        var statsLayouts = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("patternLayout",
                    () -> initLayout(new PatternLayout()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop()),
                Benchmark.of("jsonLayout",
                    () -> initLayout(new JsonLayout()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop()),
                Benchmark.of("logstashLayout",
                    () -> initLayout(new LogstashLayout()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop())
            );
        }});
        statsLayouts.forEach(System.out::println);
    }

    @Test
    @Tag("com.github.loki4j.testkit.categories.PerformanceTests")
    public void multiThreadedLayoutPerformance() throws Exception {
        var statsLayouts = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 4;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                    Benchmark.of(
                            "patternLayout",
                            () -> initLayout(new PatternLayout()),
                            Layout::doLayout,
                            layout -> {},
                            Layout::stop),
                    Benchmark.of(
                            "jsonLayout",
                            () -> initLayout(new JsonLayout()),
                            Layout::doLayout,
                            layout -> {},
                            Layout::stop),
                    Benchmark.of(
                            "logstashLayout",
                            () -> initLayout(new LogstashLayout()),
                            Layout::doLayout,
                            layout -> {},
                            Layout::stop)
            );
        }});
        statsLayouts.forEach(System.out::println);
    }

}
