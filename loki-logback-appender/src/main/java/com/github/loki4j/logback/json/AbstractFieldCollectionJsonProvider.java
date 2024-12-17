package com.github.loki4j.logback.json;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * An abstract provider that writes a collection of JSON fields
 *
 * @param <V> Type of the field value supported by this provider.
 * @param <E> Type of key-value entry served by this provider.
 * @param <T> Type of key-value collection served by this provider.
 */
public abstract class AbstractFieldCollectionJsonProvider<V, E, T extends Collection<E>> extends AbstractJsonProvider {

    /**
     * A prefix added to all the keys (field names) in this collection.
     */
    private String prefix;

    /**
     * Whether to omit the prefix and use field names from this collection as they are.
     */
    private boolean noPrefix;

    /**
     * A set of keys to exclude from JSON payload.
     * Exclude list has a precedence over include list.
     * If not specified, all keys are included.
     */
    private Set<String> excludeKeys = new HashSet<>();

    /**
     * A set of keys to include into JSON payload.
     * If not specified, all keys are included.
     */
    private Set<String> includeKeys = new HashSet<>();

    @Override
    public boolean canWrite(ILoggingEvent event) {
        var entries = extractEntries(event);
        return entries != null && !entries.isEmpty();
    }

    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        var entries = extractEntries(event);
        var firstFieldWritten = false;
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

            if (startWithSeparator || firstFieldWritten)
                writer.writeFieldSeparator();
            var name = noPrefix ? key : prefix + key;
            writeField(writer, name, value);
            firstFieldWritten = true;
        }
        return firstFieldWritten;
    }

    /**
     * Extract the collection of entries (i.e. fields) from the logging event.
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
    protected abstract V extractValue(E entry);

    /**
     * Write a field into JSON event layout.
     * @param writer JSON writer to use.
     * @param fieldName Name of the field to write.
     * @param fieldValue Value of the field to write.
     */
    protected abstract void writeField(JsonEventWriter writer, String fieldName, V fieldValue);

    public void addExclude(String key) {
        excludeKeys.add(key);
    }

    public void addInclude(String key) {
        includeKeys.add(key);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isNoPrefix() {
        return noPrefix;
    }

    public void setNoPrefix(boolean noPrefix) {
        this.noPrefix = noPrefix;
    }
}
