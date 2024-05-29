package com.github.loki4j.logback.access;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.Cache;
import com.github.loki4j.client.util.StringUtils;
import com.github.loki4j.client.util.Cache.BoundAtomicMapCache;
import com.github.loki4j.slf4j.marker.LabelMarker;

import ch.qos.logback.access.PatternLayout;
import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract class that provides basic Loki4j batch encoding functionality
 */
public abstract class AbstractLoki4jEncoder extends ContextAwareBase implements Loki4jEncoder {

    private static final String REGEX_STARTER = "regex:";
    private static final String DEFAULT_MSG_PATTERN = PatternLayout.COMBINED_PATTERN;
    
    public static final class LabelCfg {
        /**
         * Logback pattern to use for log record's label
         */
        String pattern;
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
         * An implementation of a Stream cache to use.
         */
        Cache<String, LogRecordStream> streamCache;

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        public void setKeyValueSeparator(String keyValueSeparator) {
            this.keyValueSeparator = keyValueSeparator;
        }
        public void setPairSeparator(String pairSeparator) {
            this.pairSeparator = pairSeparator;
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
    private Layout<IAccessEvent> messageLayout;

    private LogRecordStream staticLabelStream = null;

    private boolean started = false;

    public void start() {
        // init with default label pattern if not set in config
        var resolvedLblPat = label.pattern == null
            ? "host=" + context.getProperty(CoreConstants.HOSTNAME_KEY)
            : label.pattern;
        // check nopex flag
        var labelPattern = resolvedLblPat;

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
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public LogRecordStream eventToStream(IAccessEvent e) {
        if (staticLabels) {
            if (staticLabelStream == null) {
                staticLabelStream = LogRecordStream.create(extractStreamKVPairs(labelPatternLayout.doLayout(e)));
            }
            return staticLabelStream;
        }

        final var renderedLayout = labelPatternLayout.doLayout(e).intern();
        var streamKey = renderedLayout;
        return label.streamCache.get(streamKey, () -> {
            var layoutLabels = extractStreamKVPairs(renderedLayout);
            return LogRecordStream.create(layoutLabels);
        });
    }

    public String eventToMessage(IAccessEvent e) {
        return messageLayout.doLayout(e);
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    String[] extractStreamKVPairs(String stream) {
        var pairs = compiledLabelPairSeparator.split(stream);
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
                    pairs[i], stream, label.pairSeparator, label.keyValueSeparator));
            }
        }
        return Arrays.copyOf(result, pos);
    }

    String[] extractLabelsFromMarker(LabelMarker marker) {
        var labelMap = marker.getLabels();
        var markerLabels = new String[labelMap.size() * 2];
        var pos = 0;
        for (Entry<String, String> entry : labelMap.entrySet()) {
            markerLabels[pos] = entry.getKey();
            markerLabels[pos + 1] = entry.getValue();
            pos += 2;
        }
        return markerLabels;
    }

    public LabelCfg getLabel() {
        return label;
    }
    public void setLabel(LabelCfg label) {
        this.label = label;
    }

    @DefaultClass(PatternLayout.class)
    public void setMessage(Layout<IAccessEvent> message) {
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
