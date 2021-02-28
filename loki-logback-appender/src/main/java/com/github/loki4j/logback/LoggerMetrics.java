package com.github.loki4j.logback;

import java.time.Duration;
import java.util.Arrays;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/**
 * Provides an ability to report logging performance metrics using Micrometer framework
 */
public class LoggerMetrics {

    private Timer appendTimer;
    private Timer encodeTimer;
    private Timer sendTimer;

    private DistributionSummary eventsEncodedSummary;
    private DistributionSummary bytesSentSummary;

    private Counter batchesEncodedCounter;
    private Counter batchesSentCounter;
    private Counter sendErrorsCounter;
    private Counter droppedEventsCounter;

    public LoggerMetrics(String appenderName, String host) {
        var tags = Arrays.asList(
            Tag.of("appender", appenderName),
            Tag.of("host", host));

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

        sendErrorsCounter = Counter
            .builder("loki4j.send.errors")
            .description("Number of errors occurred while sending batches to Loki")
            .tags(tags)
            .register(Metrics.globalRegistry);

        droppedEventsCounter = Counter
            .builder("loki4j.drop.events")
            .description("Number of events dropped due to backpressure settings")
            .tags(tags)
            .register(Metrics.globalRegistry);
    }

    private void recordTimer(Timer timer, long startedNs) {
        timer.record(Duration.ofNanos(System.nanoTime() - startedNs));
    }

    public void eventAppended(long startedNs, boolean dropped) {
        recordTimer(appendTimer, startedNs);
        if (dropped) droppedEventsCounter.increment();
    }
    
    public void batchEncoded(long startedNs, int count) {
        recordTimer(encodeTimer, startedNs);
        eventsEncodedSummary.record(count);
        batchesEncodedCounter.increment();
    }

    public void batchSent(long startedNs, int bytesCount, boolean failed) {
        recordTimer(sendTimer, startedNs);
        bytesSentSummary.record(bytesCount);
        batchesSentCounter.increment();
        if (failed) sendErrorsCounter.increment();
    }
}
