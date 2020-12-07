package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.List;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.logback.performance.Benchmarker.Benchmark;

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
                var batches = new LogRecord[batchSize][];
                var jsonEncSta = initEnc(jsonEncoder(true, "testLabel"));
                for (int i = 0; i < batches.length; i++) {
                    batches[i] = Arrays
                        .stream(generateEvents(1_000, 10))
                        .map(e -> jsonEncSta.eventToRecord(e, new LogRecord()))
                        .toArray(LogRecord[]::new);
                }
                return batches;
            };
            this.benchmarks = List.of(
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
