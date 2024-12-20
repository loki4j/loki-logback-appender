package com.github.loki4j.logback.performance.reg_v160;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Marker;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.Cache;
import com.github.loki4j.client.util.StringUtils;
import com.github.loki4j.client.util.Cache.BoundAtomicMapCache;
import com.github.loki4j.logback.Loki4jEncoder;
import com.github.loki4j.slf4j.marker.AbstractKeyValueMarker;
import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoderOld extends ContextAwareBase implements Loki4jEncoder {

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
         * If true, exception info is not added to label.
         * If false, you should take care of proper formatting.
         */
        boolean nopex = true;
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
        public void setNopex(boolean nopex) {
            this.nopex = nopex;
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

    private Pattern compiledLabelPairSeparator;
    private Pattern compiledLabelKeyValueSeparator;

    private PatternLayout labelPatternLayout;
    private PatternLayout metadataPatternLayout;
    private Layout<ILoggingEvent> messageLayout;

    private LogRecordStream staticLabelStream = null;

    private boolean started = false;

    public void start() {
        // init with default label pattern if not set in config
        var resolvedLblPat = label.pattern == null
            ? "level=%level,host=" + context.getProperty(CoreConstants.HOSTNAME_KEY)
            : label.pattern;
        // check nopex flag
        var labelPattern = label.nopex
            ? resolvedLblPat + "%nopex"
            : resolvedLblPat;

        // check if label pair separator is RegEx or literal string
        compiledLabelPairSeparator = label.pairSeparator.startsWith(REGEX_STARTER)
            ? Pattern.compile(label.pairSeparator.substring(REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(label.pairSeparator));
        // label key-value separator supports only literal strings
        compiledLabelKeyValueSeparator = Pattern.compile(Pattern.quote(label.keyValueSeparator));

        // if streamCache is not set in the config
        if (label.streamCache == null) {
            label.streamCache = new BoundAtomicMapCache<>();
        }

        labelPatternLayout = initPatternLayout(labelPattern);
        labelPatternLayout.setContext(context);
        labelPatternLayout.start();

        if (label.structuredMetadataPattern != null) {
            metadataPatternLayout = initPatternLayout(label.structuredMetadataPattern);
            metadataPatternLayout.setContext(context);
            metadataPatternLayout.start();
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
        labelPatternLayout.stop();
        if (metadataPatternLayout != null) {
            metadataPatternLayout.stop();
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public LogRecordStream eventToStream(ILoggingEvent e) {
        if (staticLabels) {
            if (staticLabelStream == null) {
                staticLabelStream = LogRecordStream.create(extractKVPairsFromRenderedPattern(labelPatternLayout.doLayout(e)));
            }
            return staticLabelStream;
        }

        final var renderedLayout = labelPatternLayout.doLayout(e).intern();
        var streamKey = renderedLayout;
        var markerLabels = label.readMarkers && e.getMarkerList() != null
            ? extractMarkers(e.getMarkerList(), LabelMarker.class)
            : EMPTY_KV_PAIRS;
        if (markerLabels.length > 0) {
            streamKey = streamKey + "!markers!" + Arrays.toString(markerLabels);
        }

        return label.streamCache.get(streamKey, () -> {
            var layoutLabels = extractKVPairsFromRenderedPattern(renderedLayout);
            if (markerLabels == EMPTY_KV_PAIRS) {
                return LogRecordStream.create(layoutLabels);
            }
            var allLabels = Arrays.copyOf(layoutLabels, layoutLabels.length + markerLabels.length);
            System.arraycopy(markerLabels, 0, allLabels, layoutLabels.length, markerLabels.length);
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
        if (metadataPatternLayout != null) {
            var renderedMetadata = metadataPatternLayout.doLayout(e);
            patternKVs = extractKVPairsFromRenderedPattern(renderedMetadata);
        }
        if (markerKVs.length == 0 && patternKVs.length == 0) {
            return EMPTY_KV_PAIRS;
        }
        var allKVs = Arrays.copyOf(patternKVs, patternKVs.length + markerKVs.length);
        System.arraycopy(markerKVs, 0, allKVs, patternKVs.length, markerKVs.length);
        return allKVs;
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    String[] extractKVPairsFromRenderedPattern(String rendered) {
        var pairs = compiledLabelPairSeparator.split(rendered);
        var result = new String[pairs.length * 2];
        var pos = 0;
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = compiledLabelKeyValueSeparator.split(pairs[i]);
            if (kv.length == 2) {
                result[pos] = kv[0];
                result[pos + 1] = kv[1];
                pos += 2;
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to label key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], rendered, label.pairSeparator, label.keyValueSeparator));
            }
        }
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
