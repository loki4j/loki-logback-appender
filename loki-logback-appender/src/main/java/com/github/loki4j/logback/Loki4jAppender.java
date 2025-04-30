package com.github.loki4j.logback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.pipeline.AsyncBufferPipeline;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.util.StringUtils;
import com.github.loki4j.logback.extractor.Extractor;
import com.github.loki4j.logback.extractor.MarkerExtractor;
import com.github.loki4j.logback.extractor.PatternsExtractor;
import com.github.loki4j.slf4j.marker.AbstractKeyValueMarker;
import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki.
 * This class is responsible for converting Logback event to internal log record representation
 * and sending it to the pipeline.
 */
public class Loki4jAppender extends PipelineConfigAppenderBase {

    private static final String KV_REGEX_STARTER = "regex:";
    private static final String KV_PAIR_SEPARATOR = "\n";
    private static final String KV_KV_SEPARATOR = "=";
    private static final String DEFAULT_LBL_PATTERN = "source=loki4j,host=";
    private static final String DEFAULT_MSG_PATTERN = "%msg %ex";

    /**
     * Logback pattern to use for log record's label.
     */
    private String labelsPattern;
    /**
     * Logback pattern to use for log record's structured metadata.
     */
    private String structuredMetadataPattern;
    /**
     * Logback layout to use for log record's message.
     */
    private Layout<ILoggingEvent> messageLayout;

    /**
     * If true, scans each log record for attached LabelMarker to
     * add its values to record's labels.
     */
    private boolean readMarkers = false;

    /**
     * If true, the appender will print its own debug logs to stderr.
     */
    private boolean verbose = false;

    /**
     * A pipeline that does all the heavy lifting log records processing.
     */
    private AsyncBufferPipeline pipeline;

    /**
     * A counter for events dropped due to backpressure.
     */
    private AtomicLong droppedEventsCount = new AtomicLong(0L);

    private List<Extractor> labelValueExtractors = new ArrayList<>();
    private List<Extractor> metadataValueExtractors = new ArrayList<>();

    private Map<String, String> staticLabelStream = null;

    @Override
    public void start() {
        // init internal logging
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        // init labels KV extraction
        if (labelsPattern == null)
            labelsPattern = DEFAULT_LBL_PATTERN + context.getProperty(CoreConstants.HOSTNAME_KEY);
        labelValueExtractors = initExtractors(labelsPattern, LabelMarker.class);

        // init structured metadata KV extraction
        if (structuredMetadataPattern != null && !structuredMetadataPattern.isBlank()) {
            metadataValueExtractors = initExtractors(structuredMetadataPattern, StructuredMetadataMarker.class);
        }

        // init message layout
        if (messageLayout == null) {
            addWarn("No message layout specified in the config. Using PatternLayout with default settings");
            messageLayout = initPatternLayout(DEFAULT_MSG_PATTERN);
        }
        messageLayout.setContext(context);
        messageLayout.start();

        // init pipeline
        PipelineConfig pipelineConf = buildPipelineConfig();
        pipeline = new AsyncBufferPipeline(pipelineConf);
        pipeline.start();

        super.start();

        addInfo("Successfully started");
    }

    @Override
    public void stop() {
        if (!super.isStarted()) {
            return;
        }
        addInfo("Stopping...");

        super.stop();

        pipeline.stop();
        messageLayout.stop();

        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        var appended = pipeline.append(() -> eventToLogRecord(event));
        if (!appended)
            reportDroppedEvents();
    }

    LogRecord eventToLogRecord(ILoggingEvent event) {
        return LogRecord.create(
            event.getTimeStamp(),
            event.getNanoseconds() % 1_000_000, // take only nanos, not ms
            extractStream(event),
            extractMessage(event),
            extractMetadata(event));
    }

    private Map<String, String> extractStream(ILoggingEvent e) {
        if (isStaticLabels()) {
            if (staticLabelStream == null) {
                if (labelValueExtractors.size() == 1) {
                    var kvs = new LinkedHashMap<String, String>();
                    labelValueExtractors.get(0).extract(e, kvs);
                    staticLabelStream = kvs;
                } else {
                    throw new IllegalStateException("No bulk patterns allowed for static label configuration");
                }
            }
            return staticLabelStream;
        }

        var kvs = new LinkedHashMap<String, String>();
        for (var extractor : labelValueExtractors) {
            extractor.extract(e, kvs);
        }
        return kvs;
    }

    private Map<String, String> extractMetadata(ILoggingEvent e) {
        if (metadataValueExtractors.isEmpty())
            return Map.of();

        var kvs = new LinkedHashMap<String, String>();
        for (var extractor : metadataValueExtractors) {
            extractor.extract(e, kvs);
        }
        return kvs;
    }

    private String extractMessage(ILoggingEvent e) {
        return messageLayout.doLayout(e);
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    private void reportDroppedEvents() {
        var dropped = droppedEventsCount.incrementAndGet();
        if (dropped == 1
                || (dropped <= 90 && dropped % 20 == 0)
                || (dropped <= 900 && dropped % 100 == 0)
                || (dropped <= 900_000 && dropped % 1000 == 0)
                || (dropped <= 9_000_000 && dropped % 10_000 == 0)
                || (dropped <= 900_000_000 && dropped % 1_000_000 == 0)
                || dropped > 1_000_000_000) {
            addWarn(String.format(
                "Backpressure: %s messages dropped. Check `sendQueueSizeBytes` setting", dropped));
            if (dropped > 1_000_000_000) {
                addWarn(String.format(
                    "Resetting dropped message counter from %s to 0", dropped));
                droppedEventsCount.set(0L);
            }
        }
    }

    private <T extends AbstractKeyValueMarker> List<Extractor> initExtractors(String pattern, Class<T> markerClass) {
        var extractors = new ArrayList<Extractor>();
        var kvPairs = extractKVPairsFromPattern(pattern, KV_PAIR_SEPARATOR, KV_KV_SEPARATOR);
        // logback patterns' extractor
        var logbackPatterns = new LinkedHashMap<String, String>();
        kvPairs.entrySet().stream().forEach(e -> logbackPatterns.put(e.getKey(), e.getValue()));
        try {
            extractors.add(new PatternsExtractor(logbackPatterns, context));
        } catch (ScanException e) {
            throw new IllegalArgumentException("Unable to parse pattern: \"" + pattern + "\"", e);
        }
        // marker extractor
        if (readMarkers)
            extractors.add(new MarkerExtractor<>(markerClass));
        return extractors;
    }

    static Map<String, String> extractKVPairsFromPattern(String pattern, String pairSeparator, String keyValueSeparator) {
        // check if label pair separator is RegEx or literal string
        var pairSeparatorPattern = pairSeparator.startsWith(KV_REGEX_STARTER)
            ? Pattern.compile(pairSeparator.substring(KV_REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(pairSeparator));
        // label key-value separator supports only literal strings
        var keyValueSeparatorPattern = Pattern.compile(Pattern.quote(keyValueSeparator));

        var pairs = pairSeparatorPattern.split(pattern);
        var result = new LinkedHashMap<String, String>();
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = keyValueSeparatorPattern.split(pairs[i]);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], pattern, pairSeparator, keyValueSeparator));
            }
        }
        if (result.isEmpty())
            throw new IllegalArgumentException("Empty of blank patterns are not supported");
        return result;
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        pipeline.waitSendQueueIsEmpty(timeoutMs);
    }

    long droppedEventsCount() {
        return droppedEventsCount.get();
    }

    public void setLabels(String labelsPattern) {
        this.labelsPattern = labelsPattern;
    }

    public void setStructuredMetadata(String structuredMetadataPattern) {
        this.structuredMetadataPattern = structuredMetadataPattern;
    }

    @DefaultClass(PatternLayout.class)
    public void setMessage(Layout<ILoggingEvent> messageLayout) {
        this.messageLayout = messageLayout;
    }

    public void setReadMarkers(boolean readMarkers) {
        this.readMarkers = readMarkers;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
