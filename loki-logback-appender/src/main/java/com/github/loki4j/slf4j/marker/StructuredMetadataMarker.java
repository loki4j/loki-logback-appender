package com.github.loki4j.slf4j.marker;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/*
 * A SLF4J Marker implementation that allows to dynamically add structured metadata to the log record.
 */
public class StructuredMetadataMarker extends AbstractKeyValueMarker {

    private static final String name = "LOKI4J_LABEL_MARKER";

    public StructuredMetadataMarker(Supplier<Map<String, String>> metadata) {
        super(metadata);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Creates a Marker containing a set of key-value pairs, passed to Loki as structured metadata.
     * @param metadata Key-value pairs will be created at time when they are first accessed (i.e. deferred creation).
     */
    public static StructuredMetadataMarker of(Supplier<Map<String, String>> metadata) {
        return new StructuredMetadataMarker(metadata);
    }

    /**
     * Creates a Marker containing a key-value pair, passed to Loki as structured metadata.
     * @param key Key of the label (assumed to be static).
     * @param value Value od the label will be created at time when it is first accessed (i.e. deferred creation).
     */
    public static StructuredMetadataMarker of(String key, Supplier<String> value) {
        return of(() -> Collections.singletonMap(key, value.get()));
    }
    
}
