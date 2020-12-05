package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.List;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.performance.Benchmarker.Benchmark;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class EncodersTest {

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var batchSize = 1000;

        var jsonEncSta = jsonEncoder(true, "testLabel");
        var jsonEncDyn = jsonEncoder(false, "testLabel");

        var protEncSta = protobufEncoder(true, "testLabel");
        var protEncDyn = protobufEncoder(false, "testLabel");

        var encoders = List.of(jsonEncSta, jsonEncDyn, protEncSta, protEncDyn);
        encoders.forEach(e -> {
            e.setContext(new LoggerContext());
            e.start();
        });

        var stats = Benchmarker.run(new Benchmarker.Config<LogRecord[]>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var batches = new LogRecord[batchSize][];
                for (int i = 0; i < batches.length; i++) {
                    batches[i] = Arrays
                        .stream(generateEvents(1_000, 10))
                        .map(e -> jsonEncSta.eventToRecord(e, new LogRecord()))
                        .toArray(LogRecord[]::new);
                }
                return batches;
            };
            this.benchmarks = List.of(
                Benchmark.of("jsonEncSta", batch -> jsonEncSta.encode(batch)),
                Benchmark.of("jsonEncDyn", batch -> jsonEncDyn.encode(batch)),
                Benchmark.of("protEncSta", batch -> protEncSta.encode(batch)),
                Benchmark.of("protEncDyn", batch -> protEncDyn.encode(batch))
            );
        }});

        encoders.forEach(e -> e.stop());

        stats.forEach(System.out::println);
    }
    
}
