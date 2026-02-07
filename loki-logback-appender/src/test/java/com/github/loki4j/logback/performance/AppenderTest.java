package com.github.loki4j.logback.performance;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;
import com.github.loki4j.logback.Generators.AppenderWrapper;
import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.logback.Generators;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import ch.qos.logback.classic.spi.ILoggingEvent;

@Tag("performance")
public class AppenderTest {

    private final static WriterFactory STRING_WRITER = new WriterFactory(Generators::stringWriter, "text/plain");

    private static AppenderWrapper initApp(int capacity, String testLabel, WriterFactory writer) {
        var batch = batch(capacity, 60_000L);
        batch.setSendQueueMaxBytes(Long.MAX_VALUE);
        var a = appender(
            "test=" + testLabel + "\nlevel=%level\nservice_name=my-app",
            null,
            plainTextMsgLayout("%msg %ex"),
            batch,
            http(writer, dummySender()));
        a.setVerbose(false);
        a.start();
        return new AppenderWrapper(a);
    }

    @Test
    public void singleThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(capacity, "singleThreadPerformance", STRING_WRITER),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(capacity, "singleThreadPerformance", PipelineConfig.json),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(capacity, "singleThreadPerformance", PipelineConfig.protobuf),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop())
            );
        }});

        stats.forEach(System.out::println);
    }

    @Test
    public void multiThreadPerformance() throws Exception {
        var capacity = 1000;

        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 2;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("dummyAppenderWait",
                    () -> initApp(capacity, "multiThreadPerformance", STRING_WRITER),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("dummyJsonAppenderWait",
                    () -> initApp(capacity, "multiThreadPerformance", PipelineConfig.json),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("dummyProtobufAppenderWait",
                    () -> initApp(capacity, "multiThreadPerformance", PipelineConfig.protobuf),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop())
            );
        }});
        stats.forEach(System.out::println);
    }

}
