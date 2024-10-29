package com.github.loki4j.logback;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Marker;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.ArrayUtils;
import com.github.loki4j.client.util.Cache;
import com.github.loki4j.client.util.StringUtils;
import com.github.loki4j.client.util.Cache.BoundAtomicMapCache;
import com.github.loki4j.slf4j.marker.AbstractKeyValueMarker;
import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ScanException;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoder extends ContextAwareBase implements Loki4jEncoder {

    private static final String REGEX_STARTER = "regex:";
    private static final String[] EMPTY_KV_PAIRS = new String[0];
    private static final String DEFAULT_MSG_PATTERN = "l=%level c=%logger{20} t=%thread | %msg %ex";
    
    public static final class LabelCfg {
        /**
         * Logback pattern to use for log record's label
         */
        String pattern;
        /**
         * Logback pattern to use for log record's structured metadata
         */
        String structuredMetadataPattern;
        /**
         * Character sequence to use as a separator between labels.
         * If starts with "regex:" prefix, the remainder is used as a regular expression separator.
         * Otherwise, the provided char sequence is used as a separator literally.
         */
        String pairSeparator = ",";
        /**
         * Character to use as a separator between label's name and its value
         */
        String keyValueSeparator = "=";
        /**
         * If true, scans each log record for attached LabelMarker to
         * add its values to record's labels.
         */
        boolean readMarkers = false;
        /**
         * An implementation of a Stream cache to use.
         */
        Cache<String, LogRecordStream> streamCache;

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        public void setStructuredMetadataPattern(String structuredMetadataPattern) {
            this.structuredMetadataPattern = structuredMetadataPattern;
        }
        public void setKeyValueSeparator(String keyValueSeparator) {
            this.keyValueSeparator = keyValueSeparator;
        }
        public void setPairSeparator(String pairSeparator) {
            this.pairSeparator = pairSeparator;
        }
        public void setReadMarkers(boolean readMarkers) {
            this.readMarkers = readMarkers;
        }
        @DefaultClass(BoundAtomicMapCache.class)
        public void setStreamCache(Cache<String, LogRecordStream> streamCache) {
            this.streamCache = streamCache;
        }
    }

    protected final Charset charset = StandardCharsets.UTF_8;

    private LabelCfg label = new LabelCfg();

    /**
     * If true, log records in batch are sorted by timestamp.
     * If false, records will be sent to Loki in arrival order.
     * Turn this on if you see 'entry out of order' error from Loki.
     */
    private boolean sortByTime = false;

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    private volatile boolean staticLabels = false;

    private List<String> labelKeys;
    private MultiPatternExtractor<ILoggingEvent> labelValuesExtractor;

    private List<String> metadataKeys;
    private MultiPatternExtractor<ILoggingEvent> metadataValuesExtractor;

    private Layout<ILoggingEvent> messageLayout;

    private LogRecordStream staticLabelStream = null;

    private boolean started = false;

    public void start() {
        // init with default label pattern if not set in config
        var labelPattern = label.pattern == null
            ? "level=%level,host=" + context.getProperty(CoreConstants.HOSTNAME_KEY)
            : label.pattern;

        // if streamCache is not set in the config
        if (label.streamCache == null) {
            label.streamCache = new BoundAtomicMapCache<>();
        }

        // init label KV extraction
        var labelKVPatterns = extractKVPairsFromPattern(labelPattern, label.pairSeparator, label.keyValueSeparator);
        labelKeys = extractIndexesMod2(labelKVPatterns, 0);
        var labelPatterns = extractIndexesMod2(labelKVPatterns, 1);
        try {
            labelValuesExtractor = new MultiPatternExtractor<>(labelPatterns, context);
        } catch (ScanException e) {
            throw new IllegalArgumentException("Unable to parse label pattern: \"" + labelPattern + "\"", e);
        }

        // init structured metadata KV extraction
        if (label.structuredMetadataPattern != null) {
            var metadataKVPatterns = extractKVPairsFromPattern(label.structuredMetadataPattern, label.pairSeparator, label.keyValueSeparator);
            metadataKeys = extractIndexesMod2(metadataKVPatterns, 0);
            var metadataPatterns = extractIndexesMod2(metadataKVPatterns, 1);
            try {
                metadataValuesExtractor = new MultiPatternExtractor<>(metadataPatterns, context);
            } catch (ScanException e) {
                throw new IllegalArgumentException("Unable to parse structured metadata pattern: \"" + label.structuredMetadataPattern + "\"", e);
            }
        }

        if (messageLayout == null) {
            addWarn("No message layout specified in the config. Using PatternLayout with default settings");
            messageLayout = initPatternLayout(DEFAULT_MSG_PATTERN);
        }
        messageLayout.setContext(context);
        messageLayout.start();

        this.started = true;
    }

    public void stop() {
        this.started = false;
        messageLayout.stop();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public LogRecordStream eventToStream(ILoggingEvent e) {
        if (staticLabels) {
            if (staticLabelStream == null) {
                staticLabelStream = LogRecordStream.create(mergeKVPairs(labelKeys, labelValuesExtractor.extract(e)));
            }
            return staticLabelStream;
        }

        final var labelValues = labelValuesExtractor.extract(e);
        var markerLabels = label.readMarkers && e.getMarkerList() != null
            ? extractMarkers(e.getMarkerList(), LabelMarker.class)
            : EMPTY_KV_PAIRS;
        var streamKey = ArrayUtils.join2(labelValues, markerLabels, " !%! ");//.intern();
        return label.streamCache.get(streamKey, () -> {
            var layoutLabels = mergeKVPairs(labelKeys, labelValues);
            var allLabels = ArrayUtils.concat(layoutLabels, markerLabels);
            return LogRecordStream.create(allLabels);
        });
    }

    public String eventToMessage(ILoggingEvent e) {
        return messageLayout.doLayout(e);
    }

    public String[] eventToMetadata(ILoggingEvent e) {
        var markerKVs = label.readMarkers && e.getMarkerList() != null
            ? extractMarkers(e.getMarkerList(), StructuredMetadataMarker.class)
            : EMPTY_KV_PAIRS;
        var patternKVs = EMPTY_KV_PAIRS;
        if (metadataValuesExtractor != null) {
            var metadataValues = metadataValuesExtractor.extract(e);
            patternKVs = mergeKVPairs(metadataKeys, metadataValues);
        }
        var allKVs = ArrayUtils.concat(patternKVs, markerKVs);
        return allKVs;
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    static String[] extractKVPairsFromPattern(String pattern, String pairSeparator, String keyValueSeparator) {
        // check if label pair separator is RegEx or literal string
        var pairSeparatorPattern = pairSeparator.startsWith(REGEX_STARTER)
            ? Pattern.compile(pairSeparator.substring(REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(pairSeparator));
        // label key-value separator supports only literal strings
        var keyValueSeparatorPattern = Pattern.compile(Pattern.quote(keyValueSeparator));

        var pairs = pairSeparatorPattern.split(pattern);
        var result = new String[pairs.length * 2];
        var pos = 0;
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = keyValueSeparatorPattern.split(pairs[i]);
            if (kv.length == 2) {
                result[pos] = kv[0].trim();
                result[pos + 1] = kv[1].trim();
                pos += 2;
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], pattern, pairSeparator, keyValueSeparator));
            }
        }
        if (pos == 0)
            throw new IllegalArgumentException("Empty of blank patterns are not supported");
        // we skip blank pairs, so need to shrink the array to the actual size
        return Arrays.copyOf(result, pos);
    }

    private <T extends AbstractKeyValueMarker> String[] extractMarkers(List<Marker> markers, Class<T> clazz) {
        for (var marker: markers) {
            if (marker != null && clazz.isAssignableFrom(marker.getClass())) {
                return extractKVPairsFromMarker((AbstractKeyValueMarker) marker);  // only one Marker is supported per event
            }
        }
        return EMPTY_KV_PAIRS;
    }

    private String[] extractKVPairsFromMarker(AbstractKeyValueMarker marker) {
        var map = marker.getKeyValuePairs();
        var markerKVPairs = new String[map.size() * 2];
        var pos = 0;
        for (Entry<String, String> entry : map.entrySet()) {
            markerKVPairs[pos] = entry.getKey();
            markerKVPairs[pos + 1] = entry.getValue();
            pos += 2;
        }
        return markerKVPairs;
    }

    private static List<String> extractIndexesMod2(String[] array, int mod2) {
        return IntStream.range(0, array.length)
            .filter(i -> i % 2 == mod2)
            .mapToObj(i -> array[i])
            .collect(Collectors.toList());
    }

    private static String[] mergeKVPairs(List<String> keys, String[] values) {
        var resultLen = values.length * 2;
        var result = new String[resultLen];
        for (int i = 0; i < resultLen; i += 2) {
            result[i] = keys.get(i / 2);
            result[i + 1] = values[i / 2];
        }
        return result;
    }

    public LabelCfg getLabel() {
        return label;
    }
    public void setLabel(LabelCfg label) {
        this.label = label;
    }

    @DefaultClass(PatternLayout.class)
    public void setMessage(Layout<ILoggingEvent> message) {
        this.messageLayout = message;
    }

    public boolean getSortByTime() {
        return sortByTime;
    }
    public void setSortByTime(boolean sortByTime) {
        this.sortByTime = sortByTime;
    }

    public boolean getStaticLabels() {
        return staticLabels;
    }
    public void setStaticLabels(boolean staticLabels) {
        this.staticLabels = staticLabels;
    }

}
