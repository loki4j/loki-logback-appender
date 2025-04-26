package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.writer.JsonWriter;
import com.github.loki4j.client.writer.ProtobufWriter;
import com.github.loki4j.client.writer.Writer;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class WritersTest {

    private static int CAPACITY_BYTES = 4 * 1024 * 1024;

    private ByteBuffer resultBuffer = ByteBuffer.allocate(CAPACITY_BYTES);

    private static AbstractLoki4jEncoder initEnc() {
        var e = new AbstractLoki4jEncoder();
        e.setStaticLabels(true);
        e.setContext(new LoggerContext());
        e.start();
        return e;
    }

    private void writeBatch(Writer w, LogRecordBatch b) {
        w.serializeBatch(b);
        w.toByteBuffer(resultBuffer);
        resultBuffer.clear();
    }

    @Test
    @Category({PerformanceTests.class})
    public void serializePerformance() throws Exception {
        var batchSize = 1000;

        var statsSta = Benchmarker.run(new Benchmarker.Config<LogRecordBatch>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var jsonEncSta = initEnc();
                return Stream.iterate(
                        Arrays.stream(generateEvents(batchSize, 10))
                            .map(e -> eventToRecord(e, jsonEncSta))
                            .toArray(LogRecord[]::new),
                        UnaryOperator.identity())
                    .map(rs -> new LogRecordBatch(rs))
                    .limit(1000)
                    .iterator();
            };
            this.benchmarks = Arrays.asList(
                Benchmark.of("defaultEncSta",
                    () -> stringWriter(CAPACITY_BYTES, new ByteBufferFactory(false)),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("jsonEncSta",
                    () -> new JsonWriter(CAPACITY_BYTES),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("protEncSta",
                    () -> new ProtobufWriter(CAPACITY_BYTES, new ByteBufferFactory(false)),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("protEncOffHeap",
                    () -> new ProtobufWriter(CAPACITY_BYTES, new ByteBufferFactory(true)),
                    (w, batch) -> writeBatch(w, batch)));
        }});
        statsSta.forEach(System.out::println);

        var statsDyn = Benchmarker.run(new Benchmarker.Config<LogRecordBatch>() {{
            this.runs = 50;
            this.parFactor = 1;
            this.generator = () -> {
                var jsonEncDyn = initEnc();
                return Stream.iterate(
                        Arrays.stream(generateEvents(batchSize, 10))
                            .map(e -> eventToRecord(e, jsonEncDyn))
                            .toArray(LogRecord[]::new),
                        UnaryOperator.identity())
                    .map(rs -> new LogRecordBatch(rs))
                    .map(b -> {
                        b.sort((e1, e2) -> Long.compare(e1.stream.hashCode(), e2.stream.hashCode()));
                        return b;
                    })
                    .limit(1000)
                    .iterator();
            };
            this.benchmarks = Arrays.asList(
                Benchmark.of("defaultEncDyn",
                    () -> stringWriter(CAPACITY_BYTES, new ByteBufferFactory(false)),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("jsonEncDyn",
                    () -> new JsonWriter(CAPACITY_BYTES),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("protEncDyn",
                    () -> new ProtobufWriter(CAPACITY_BYTES, new ByteBufferFactory(false)),
                    (w, batch) -> writeBatch(w, batch)),
                Benchmark.of("protEncDynOffHeap",
                    () -> new ProtobufWriter(CAPACITY_BYTES, new ByteBufferFactory(true)),
                    (w, batch) -> writeBatch(w, batch)));
        }});
        statsDyn.forEach(System.out::println);
    }

}
