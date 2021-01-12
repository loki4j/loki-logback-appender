package com.github.loki4j.logback.performance.reg_v110;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LokiResponse;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/**
 * Extends {@code Loki4jAppender} with ability to report its
 * performance metrics via Micrometer framework.
 */
public class InstrumentedLoki4jAppender extends Loki4jAppenderV100 {

    private Timer appendTimer;
    private Timer encodeTimer;
    private Timer sendTimer;

    private DistributionSummary eventsEncodedSummary;
    private DistributionSummary bytesSentSummary;

    private Counter batchesEncodedCounter;
    private Counter batchesSentCounter;

    @Override
    public void start() {
        super.start();

        var host = context.getProperty(CoreConstants.HOSTNAME_KEY);
        var tags = Arrays.asList(
            Tag.of("appender", this.getName() == null ? "none" : this.getName()),
            Tag.of("host", host == null ? "unknown" : host));

        appendTimer = Timer
            .builder("loki4j.append.time")
            .description("Time for a single event append operation")
            .tags(tags)
            .register(Metrics.globalRegistry);

        encodeTimer = Timer
            .builder("loki4j.encode.time")
            .description("Time for a batch encode operation")
            .tags(tags)
            .register(Metrics.globalRegistry);

        sendTimer = Timer
            .builder("loki4j.send.time")
            .description("Time for a HTTP send operation")
            .tags(tags)
            .register(Metrics.globalRegistry);

        eventsEncodedSummary = DistributionSummary
            .builder("loki4j.encode.events")
            .description("Number of log events processed by encoder")
            .tags(tags)
            .register(Metrics.globalRegistry);

        bytesSentSummary = DistributionSummary
            .builder("loki4j.send.bytes")
            .description("Size of batches sent to Loki")
            .baseUnit("bytes")
            .tags(tags)
            .register(Metrics.globalRegistry);

        batchesEncodedCounter = Counter
            .builder("loki4j.encode.batches")
            .description("Number of batches processed by encoder")
            .tags(tags)
            .register(Metrics.globalRegistry);

        batchesSentCounter = Counter
            .builder("loki4j.send.batches")
            .description("Number of batches sent to Loki")
            .tags(tags)
            .register(Metrics.globalRegistry);
    }

    private void recordTimer(Timer timer, long startedNs) {
        timer.record(Duration.ofNanos(System.nanoTime() - startedNs));
    }

    @Override
    protected void append(ILoggingEvent event) {
        var startedNs = System.nanoTime();
        super.append(event);
        recordTimer(appendTimer, startedNs);
    }

    @Override
    protected byte[] encode(LogRecord[] batch) {
        var startedNs = System.nanoTime();
        var encoded = super.encode(batch);
        recordTimer(encodeTimer, startedNs);
        eventsEncodedSummary.record(batch.length);
        batchesEncodedCounter.increment();
        return encoded;
    }

    @Override
    protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        var startedNs = System.nanoTime();
        return super
            .sendAsync(batch)
            .whenComplete((r, e) -> {
                recordTimer(sendTimer, startedNs);
                bytesSentSummary.record(batch.length);
                batchesSentCounter.increment();
            });
    }
    
}
