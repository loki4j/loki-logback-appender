package com.github.loki4j.logback.performance.reg_v200;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.logback.AbstractLoki4jEncoder.LabelCfg;
import com.github.loki4j.logback.Generators.AppenderWrapper;
import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ArrayVsMapTest {

    private static AppenderWrapper initApp(LabelCfg labelCfg) {
        var e = toStringEncoder(
            labelCfg,
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false);
        var a = appender(1000, 60_000L, e, dummySender());
        a.setSendQueueMaxBytes(Long.MAX_VALUE);
        a.setVerbose(false);
        a.start();
        return new AppenderWrapper(a);
    }


    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("maps-new2",
                    () -> initApp(labelMetadataCfg("app=my-app", "t=%thread,c=%logger", true)),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop())
            );
        }});

        stats.forEach(System.out::println);
    }
}
