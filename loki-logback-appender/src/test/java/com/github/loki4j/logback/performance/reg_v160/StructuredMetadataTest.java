package com.github.loki4j.logback.performance.reg_v160;

import static com.github.loki4j.logback.Generators.*;

import java.util.Arrays;

import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.logback.Generators;
import com.github.loki4j.logback.Loki4jEncoder;
import com.github.loki4j.logback.AbstractLoki4jEncoder.LabelCfg;
import com.github.loki4j.logback.Generators.AppenderWrapper;
import com.github.loki4j.logback.Generators.InfiniteEventIterator;
import com.github.loki4j.testkit.benchmark.Benchmarker;
import com.github.loki4j.testkit.benchmark.Benchmarker.Benchmark;
import com.github.loki4j.testkit.categories.PerformanceTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;

public class StructuredMetadataTest {

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

    private static AppenderWrapper initOldApp(AbstractLoki4jEncoderOld.LabelCfg labelCfg) {
        var e = wrapToOldEncoder(
            labelCfg,
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            true,
            false);
        var a = appender(1000, 60_000L, e, dummySender());
        a.setSendQueueMaxBytes(Long.MAX_VALUE);
        a.setVerbose(false);
        a.start();
        return new AppenderWrapper(a);
    }

    public static Loki4jEncoder wrapToOldEncoder(
            AbstractLoki4jEncoderOld.LabelCfg label,
            Layout<ILoggingEvent> messageLayout,
            boolean sortByTime,
            boolean staticLabels) {
        var encoder = new AbstractLoki4jEncoderOld() {
            @Override
            public PipelineConfig.WriterFactory getWriterFactory() {
                return new PipelineConfig.WriterFactory(Generators::stringWriter, "text/plain");
            }
        };
        encoder.setLabel(label);
        encoder.setMessage(messageLayout);
        encoder.setSortByTime(sortByTime);
        encoder.setStaticLabels(staticLabels);
        return encoder;
    }

    public static AbstractLoki4jEncoderOld.LabelCfg labelCfgOld(
            String pattern,
            String pairSeparator,
            String keyValueSeparator,
            boolean nopex,
            boolean readMarkers) {
        var label = new AbstractLoki4jEncoderOld.LabelCfg();
        label.setPattern(pattern);
        label.setPairSeparator(pairSeparator);
        label.setKeyValueSeparator(keyValueSeparator);
        label.setNopex(nopex);
        label.setReadMarkers(readMarkers);
        return label;
    }

    public static AbstractLoki4jEncoderOld.LabelCfg labelMetadataCfgOld(
            String pattern,
            String metadataPattern,
            boolean readMarkers) {
        var label = new AbstractLoki4jEncoderOld.LabelCfg();
        label.setPattern(pattern);
        label.setStructuredMetadataPattern(metadataPattern);
        label.setReadMarkers(readMarkers);
        return label;
    }

    @Test
    @Category({PerformanceTests.class})
    public void singleThreadPerformance() throws Exception {
        var stats = Benchmarker.run(new Benchmarker.Config<ILoggingEvent>() {{
            this.runs = 100;
            this.parFactor = 1;
            this.generator = () -> InfiniteEventIterator.from(generateEvents(10_000, 10)).limited(100_000);
            this.benchmarks = Arrays.asList(
                Benchmark.of("noStructuredMetadata-old",
                    () -> initOldApp(labelCfgOld("app=my-app", ",", "=", true, true)),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("structuredMetadataPattern-old",
                    () -> initOldApp(labelMetadataCfgOld("app=my-app", "t=%thread,c=%logger", true)),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("noStructuredMetadata-new",
                    () -> initApp(labelCfg("app=my-app", ",", "=", true, true)),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop()),
                Benchmark.of("structuredMetadataPattern-new",
                    () -> initApp(labelMetadataCfg("app=my-app", "t=%thread,c=%logger", true)),
                    (a, e) -> a.append(e),
                    a -> a.waitAllAppended(),
                    a -> a.stop())
            );
        }});

        stats.forEach(System.out::println);
    }
}
