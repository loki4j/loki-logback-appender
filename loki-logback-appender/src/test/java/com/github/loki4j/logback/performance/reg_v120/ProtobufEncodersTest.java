package com.github.loki4j.logback.performance.reg_v120;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class ProtobufEncodersTest {

    private static AbstractLoki4jEncoder initEnc(AbstractLoki4jEncoder e, boolean isDirect) {
        e.setCapacity(4 * 1024 * 1024);
        e.setBufferFactory(new ByteBufferFactory(isDirect));
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
                var jsonEncSta = initEnc(jsonEncoder(true, "testLabel"), false);
                return Stream.iterate(
                        Arrays.stream(generateEvents(batchSize, 10))
                            .map(e -> jsonEncSta.eventToRecord(e))
                            .toArray(LogRecord[]::new),
                        UnaryOperator.identity())
                    .limit(1000)
                    .iterator();
            };
            this.benchmarks = Arrays.asList(
                Benchmark.of("oldProtEnc",
                    () -> {
                        var encoder = new ProtobufEncoderV110();
                        encoder.setStaticLabels(false);
                        encoder.setLabel(labelCfg("test=testLabel,level=%level,app=my-app", ",", "=", true));
                        return initEnc(encoder, false);
                    },
                    (e, batch) -> e.encode(new LogRecordBatch(batch))),
                Benchmark.of("newProtEncOnHeap",
                    () -> initEnc(protobufEncoder(false, "testLabel"), false),
                    (e, batch) -> e.encode(new LogRecordBatch(batch))),
                Benchmark.of("newProtEncOffHeap",
                    () -> initEnc(protobufEncoder(false, "testLabel"), true),
                    (e, batch) -> e.encode(new LogRecordBatch(batch)))
            );
        }});

        stats.forEach(System.out::println);
    }

}
