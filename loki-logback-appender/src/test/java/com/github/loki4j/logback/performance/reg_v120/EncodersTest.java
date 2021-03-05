package com.github.loki4j.logback.performance.reg_v120;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.logback.JsonEncoder;
import com.github.loki4j.logback.Loki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class EncodersTest {

    private static Loki4jEncoder initEnc(Loki4jEncoder e, boolean isDirect) {
        e.setCapacity(4 * 1024 * 1024);
        e.setBufferFactory(new ByteBufferFactory(isDirect));
        e.setContext(new LoggerContext());
        e.start();
        return e;
    }

    @Test
    @Category({PerformanceTests.class})
    public void protobufWriterPerformance() throws Exception {
        var batchSize = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<LogRecord[]>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var jsonEncSta = initEnc(jsonEncoder(false, "testLabel"), false);
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
                        encoder.setLabel(AbstractLoki4jEncoderV110.labelCfg("test=testLabel,level=%level,app=my-app", ",", "=", true));
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

    @Test
    @Category({PerformanceTests.class})
    public void staticOnDynamicLabelsPerformance() throws Exception {
        var batchSize = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<LogRecord[]>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var enc = new JsonEncoder();
                enc.setStaticLabels(true);
                enc.setLabel(labelCfg("test=testLabel,app=my-app", ",", "=", true));
                var jsonEncSta = initEnc(enc, false);
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
                        var e = new ProtobufEncoderV110();
                        e.setStaticLabels(true);
                        e.setLabel(AbstractLoki4jEncoderV110.labelCfg("test=testLabel,app=my-app", ",", "=", true));
                        return initEnc(e, false);
                    },
                    (e, batch) -> e.encode(new LogRecordBatch(batch))),
                Benchmark.of("newProtEnc",
                    () -> {
                        var e = protobufEncoder(true, "testLabel");
                        e.setLabel(labelCfg("test=testLabel,app=my-app", ",", "=", true));
                        return initEnc(e, false);
                    },
                    (e, batch) -> e.encode(new LogRecordBatch(batch))),
                Benchmark.of("oldJsonEnc",
                    () -> {
                        var e = new JsonEncoderV120a();
                        e.setStaticLabels(true);
                        e.setLabel(AbstractLoki4jEncoderV110.labelCfg("test=testLabel,app=my-app", ",", "=", true));
                        return initEnc(e, false);
                    },
                    (e, batch) -> e.encode(new LogRecordBatch(batch))),
                Benchmark.of("newJsonEnc",
                    () -> {
                        var e = jsonEncoder(true, "testLabel");
                        e.setLabel(labelCfg("test=testLabel,app=my-app", ",", "=", true));
                        return initEnc(e, false);
                    },
                    (e, batch) -> e.encode(new LogRecordBatch(batch)))
            );
        }});

        stats.forEach(System.out::println);
    }

}
