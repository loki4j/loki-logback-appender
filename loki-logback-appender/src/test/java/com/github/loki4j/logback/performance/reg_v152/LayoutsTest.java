package com.github.loki4j.logback.performance.reg_v152;

import static com.github.loki4j.logback.Generators.generateEvents;

import java.util.Arrays;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;

public class LayoutsTest {

    private static Layout<ILoggingEvent> initLayout(Layout<ILoggingEvent> l) {
        l.setContext(new LoggerContext());
        l.start();
        return l;
    }
    
    @Test
    @Category({PerformanceTests.class})
    public void layoutPerformance() throws Exception {
        var statsLayouts = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 4;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(100_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("oldJsonLayout",
                    () -> initLayout(new JsonLayoutOld()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop()),
                Benchmark.of("threadLocalJsonLayout",
                    () -> initLayout(new JsonLayoutThreadLocal()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop()),
                Benchmark.of("newAllocJsonLayout",
                    () -> initLayout(new JsonLayoutNewAlloc()),
                    (layout, e) -> layout.doLayout(e),
                    layout -> {},
                    layout -> layout.stop())
            );
        }});
        statsLayouts.forEach(System.out::println);
    }

}
