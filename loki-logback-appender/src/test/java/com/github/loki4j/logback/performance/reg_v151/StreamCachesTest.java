package com.github.loki4j.logback.performance.reg_v151;

import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.Cache;

public class StreamCachesTest {

    private static AbstractLoki4jEncoder initEncoder(Cache<Map<String, String>, LogRecordStream> streamCache) {
        var e = toStringEncoder(
            labelCfg("level=%level,app=my-app,date=%date{HH:mm:ss.SSS}", ",", "=", true, false),
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false);
        e.setContext(new LoggerContext());
        e.getLabel().setStreamCache(streamCache);
        e.start();
        return e;
    }

    @Test
    @Category({PerformanceTests.class})
    public void multiThreadPerformance() throws Exception {
        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 2;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(100, 10)).limited(80_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("BoundAtomicMapCache",
                    () -> initEncoder(new Cache.BoundAtomicMapCache<>()),
                    (encoder, e) -> encoder.eventToStream(e)),
                Benchmark.of("UnboundAtomicMapCache",
                    () -> initEncoder(new UnboundAtomicMapCache<>()),
                    (encoder, e) -> encoder.eventToStream(e))
            );
        }});
        stats.forEach(System.out::println);
    }

}
