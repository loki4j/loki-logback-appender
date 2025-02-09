package com.github.loki4j.logback;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.Cache;
import com.github.loki4j.client.util.StringUtils;
import com.github.loki4j.client.util.Cache.BoundAtomicMapCache;
import com.github.loki4j.logback.extractor.Extractor;
import com.github.loki4j.logback.extractor.MarkerExtractor;
import com.github.loki4j.logback.extractor.MetadataExtractor;
import com.github.loki4j.logback.extractor.PatternsExtractor;
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
        Cache<Map<String, String>, LogRecordStream> streamCache;

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
        public void setStreamCache(Cache<Map<String, String>, LogRecordStream> streamCache) {
            this.streamCache = streamCache;
        }
    }

    protected final Charset charset = StandardCharsets.UTF_8;

    private LabelCfg label = new LabelCfg();

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    private volatile boolean staticLabels = false;

    private List<Extractor> labelValueExtractors = new ArrayList<>();
    private List<Extractor> metadataValueExtractors = new ArrayList<>();

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
        labelValueExtractors = initExtractors(labelPattern, LabelMarker.class);

        // init structured metadata KV extraction
        if (label.structuredMetadataPattern != null) {
            metadataValueExtractors = initExtractors(label.structuredMetadataPattern, StructuredMetadataMarker.class);
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
                if (labelValueExtractors.size() == 1) {
                    var kvs = new LinkedHashMap<String, String>();
                    labelValueExtractors.get(0).extract(e, kvs);
                    var kva = map2KVPairs(kvs);
                    staticLabelStream = LogRecordStream.create(kva);
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
        return label.streamCache.get(kvs, () -> {
            var allLabels = map2KVPairs(kvs);
            return LogRecordStream.create(allLabels);
        });
    }

    public String eventToMessage(ILoggingEvent e) {
        return messageLayout.doLayout(e);
    }

    public String[] eventToMetadata(ILoggingEvent e) {
        var kvs = new LinkedHashMap<String, String>();
        for (var extractor : metadataValueExtractors) {
            extractor.extract(e, kvs);
        }
        var allKVs = map2KVPairs(kvs);
        return allKVs;
    }

    private PatternLayout initPatternLayout(String pattern) {
        var patternLayout = new PatternLayout();
        patternLayout.setPattern(pattern);
        return patternLayout;
    }

    private <T extends AbstractKeyValueMarker> List<Extractor> initExtractors(String pattern, Class<T> markerClass) {
        var extractors = new ArrayList<Extractor>();
        var kvPairs = extractKVPairsFromPattern(pattern, label.pairSeparator, label.keyValueSeparator);
        Predicate<Entry<String, String>> bulkPatternPredicate = e ->
            e.getValue().startsWith("%%") && e.getKey().contains("*"); // simple check on this level
        // logback patterns
        var logbackPatterns = new LinkedHashMap<String, String>();
        kvPairs.stream().filter(bulkPatternPredicate.negate()).forEach(e -> logbackPatterns.put(e.getKey(), e.getValue()));
        try {
            extractors.add(new PatternsExtractor(logbackPatterns, context));
        } catch (ScanException e) {
            throw new IllegalArgumentException("Unable to parse pattern: \"" + pattern + "\"", e);
        }
        // bulk patterns
        var bulkPatterns = kvPairs.stream().filter(bulkPatternPredicate).collect(Collectors.toList());
        for (var bulkPattern : bulkPatterns) {
            var keyMatch = Pattern.compile("^(.*)\\*[ ]*(!)?$").matcher(bulkPattern.getKey());
            if (!keyMatch.find())
                throw new IllegalArgumentException("Unable to parse bulk pattern key: " + bulkPattern.getKey());
            var prefix = keyMatch.group(1).trim();
            var neg = keyMatch.group(2) != null;

            var valueMatch = Pattern.compile("^%%([^{]+)(\\{([^}]*)\\})?$").matcher(bulkPattern.getValue());
            if (!valueMatch.find())
                throw new IllegalArgumentException("Unable to parse bulk pattern value: " + bulkPattern.getValue());
            var func = valueMatch.group(1).trim();
            var paramsStr = valueMatch.group(3);
            var params = paramsStr != null && !paramsStr.isBlank()
                ? Arrays.stream(valueMatch.group(3).split(",")).map(String::trim).collect(Collectors.toSet())
                : Set.<String>of();
            var include = !neg ? params : Set.<String>of();
            var exclude = neg ? params : Set.<String>of();

            if (func.equalsIgnoreCase("mdc"))
                extractors.add(MetadataExtractor.mdc(prefix, include, exclude));
            else if (func.equalsIgnoreCase("kvp"))
                extractors.add(MetadataExtractor.kvp(pattern, include, exclude));
            else if (func.equalsIgnoreCase("marker")) {

            } else
                throw new IllegalArgumentException(
                    String.format("Unknown function '%s' used for bulk pattern: %s", func, bulkPattern.getValue()));
        }
        if (label.readMarkers)
            extractors.add(new MarkerExtractor<>(markerClass));
        return extractors;
    }

    static List<Entry<String, String>> extractKVPairsFromPattern(String pattern, String pairSeparator, String keyValueSeparator) {
        // check if label pair separator is RegEx or literal string
        var pairSeparatorPattern = pairSeparator.startsWith(REGEX_STARTER)
            ? Pattern.compile(pairSeparator.substring(REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(pairSeparator));
        // label key-value separator supports only literal strings
        var keyValueSeparatorPattern = Pattern.compile(Pattern.quote(keyValueSeparator));

        var pairs = pairSeparatorPattern.split(pattern);
        var result = new ArrayList<Entry<String, String>>();
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = keyValueSeparatorPattern.split(pairs[i]);
            if (kv.length == 2) {
                result.add(Map.entry(kv[0].trim(), kv[1].trim()));
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

    private static String[] map2KVPairs(Map<String, String> map) {
        var resultLen = map.size() * 2;
        var result = new String[resultLen];
        var pos = 0;
        for (var kv : map.entrySet()) {
            result[pos] = kv.getKey();
            result[pos + 1] = kv.getValue();
            pos += 2;
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

    public boolean getStaticLabels() {
        return staticLabels;
    }
    public void setStaticLabels(boolean staticLabels) {
        this.staticLabels = staticLabels;
    }

}
