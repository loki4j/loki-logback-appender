package com.github.loki4j.logback.extractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A component for extracting multiple key-value pairs from Logback event's
 * metadata (mdc, kvp).
 * Used for effective label/metadata extraction.
 * 
 * @param <E> Type of key-value entry served by this provider.
 * @param <T> Type of key-value collection served by this provider.
 */
public abstract class MetadataExtractor<E, T extends Collection<E>> implements Extractor {

    private final String prefix;
    private final boolean noPrefix;

    private Set<String> excludeKeys = new HashSet<>();
    private Set<String> includeKeys = new HashSet<>();

    MetadataExtractor(String prefix, Set<String> includeKeys, Set<String> excludeKeys) {
        this.prefix = prefix;
        this.noPrefix = prefix == null || prefix.length() == 0;
        this.excludeKeys = excludeKeys;
        this.includeKeys = includeKeys;
    }

    /**
     * Extracts key-value pairs from the Logback event provided.
     */
    public void extract(ILoggingEvent event, Map<String, String> result) {
        var entries = extractEntries(event);
        if (entries == null || entries.isEmpty())
            return;

        for (E entry : entries) {
            var key = extractKey(entry);
            var value = extractValue(entry);
            // skip empty records
            if (key == null || value == null)
                continue;

            // check exclude list, if defined
            if (!excludeKeys.isEmpty() && excludeKeys.contains(key))
                continue;

            // check include list, if defined
            if (!includeKeys.isEmpty() && !includeKeys.contains(key))
                continue;

            result.put(noPrefix ? key : prefix + key, value);
        }
    }

    /**
     * Extractor for MDC.
     */
    public static MdcBulkPatternExtractor mdc(String prefix, Set<String> excludeKeys, Set<String> includeKeys) {
        return new MdcBulkPatternExtractor(prefix, excludeKeys, includeKeys);
    }

    /**
     * Extractor for KVP.
     */
    public static KvpBulkPatternExtractor kvp(String prefix, Set<String> excludeKeys, Set<String> includeKeys) {
        return new KvpBulkPatternExtractor(prefix, excludeKeys, includeKeys);
    }

    /**
     * Extract the collection of entries (i.e. fields) from the logging event.
     * 
     * @param event Current logback event.
     */
    protected abstract T extractEntries(ILoggingEvent event);

    /**
     * Extract the key (i.e. field name) from the collection entry.
     */
    protected abstract String extractKey(E entry);

    /**
     * Extract the value (i.e. field value) from the collection entry.
     */
    protected abstract String extractValue(E entry);

    public static class MdcBulkPatternExtractor
            extends MetadataExtractor<Map.Entry<String, String>, Set<Map.Entry<String, String>>> {
        MdcBulkPatternExtractor(String prefix, Set<String> excludeKeys, Set<String> includeKeys) {
            super(prefix, excludeKeys, includeKeys);
        }

        @Override
        protected Set<Entry<String, String>> extractEntries(ILoggingEvent event) {
            var mdcProperties = event.getMDCPropertyMap();
            return mdcProperties == null ? null : mdcProperties.entrySet();
        }

        @Override
        protected String extractKey(Entry<String, String> entry) {
            return entry.getKey();
        }

        @Override
        protected String extractValue(Entry<String, String> entry) {
            return entry.getValue();
        }
    }

    public static class KvpBulkPatternExtractor extends MetadataExtractor<KeyValuePair, List<KeyValuePair>> {
        KvpBulkPatternExtractor(String prefix, Set<String> excludeKeys, Set<String> includeKeys) {
            super(prefix, excludeKeys, includeKeys);
        }

        @Override
        protected List<KeyValuePair> extractEntries(ILoggingEvent event) {
            return event.getKeyValuePairs();
        }

        @Override
        protected String extractKey(KeyValuePair entry) {
            return entry.key;
        }

        @Override
        protected String extractValue(KeyValuePair entry) {
            return entry.value == null ? null : entry.value.toString();
        }
    }
}
