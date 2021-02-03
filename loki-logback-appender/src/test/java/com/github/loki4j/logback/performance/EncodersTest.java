package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class EncodersTest {

    private static AbstractLoki4jEncoder initEnc(AbstractLoki4jEncoder e) {
        e.setContext(new LoggerContext());
        e.start();
        return e;
    }

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var batchSize = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<LogRecord[]>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var jsonEncSta = initEnc(jsonEncoder(true, "testLabel"));
                return Stream.iterate(
                        Arrays.stream(generateEvents(batchSize, 10))
                            .map(e -> jsonEncSta.eventToRecord(e))
                            .toArray(LogRecord[]::new),
                        UnaryOperator.identity())
                    .limit(1000)
                    .iterator();
            };
            this.benchmarks = Arrays.asList(
                Benchmark.of("defaultEnc",
                    () -> initEnc(defaultToStringEncoder()),
                    (e, batch) -> e.encode(batch)),
                Benchmark.of("jsonEncSta",
                    () -> initEnc(jsonEncoder(true, "testLabel")),
                    (e, batch) -> e.encode(batch)),
                Benchmark.of("jsonEncDyn",
                    () -> initEnc(jsonEncoder(false, "testLabel")),
                    (e, batch) -> e.encode(batch)),
                Benchmark.of("protEncSta",
                    () -> initEnc(protobufEncoder(true, "testLabel")),
                    (e, batch) -> e.encode(batch)),
                Benchmark.of("protEncDyn",
                    () -> initEnc(protobufEncoder(false, "testLabel")),
                    (e, batch) -> e.encode(batch))
            );
        }});

        stats.forEach(System.out::println);
    }

}
