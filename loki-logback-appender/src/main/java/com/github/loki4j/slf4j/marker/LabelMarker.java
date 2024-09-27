package com.github.loki4j.slf4j.marker;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/*
 * A SLF4J Marker implementation that allows to dynamically add Loki labels to the log record.
 */
public class LabelMarker extends AbstractKeyValueMarker {

    private static final String name = "LOKI4J_LABEL_MARKER";

    public LabelMarker(Supplier<Map<String, String>> labelsSupplier) {
        super(labelsSupplier);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Creates a Marker containing a set of Loki labels, where each label is a key-value pair.
     * @param labels Labels will be created at time when they are first accessed (i.e. deferred creation).
     */
    public static StructuredMetadataMarker of(Supplier<Map<String, String>> labels) {
        return new StructuredMetadataMarker(labels);
    }

    /**
     * Creates a Marker containing one Loki label, that is a key-value pair.
     * @param key Key of the label (assumed to be static).
     * @param value Value od the label will be created at time when it is first accessed (i.e. deferred creation).
     */
    public static StructuredMetadataMarker of(String key, Supplier<String> value) {
        return of(() -> Collections.singletonMap(key, value.get()));
    }
    
}
