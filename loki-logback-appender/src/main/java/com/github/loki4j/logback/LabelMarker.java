package com.github.loki4j.logback;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Marker;

/*
 * A SLF4J Marker implementation that allows to dynamically add Loki lables to the log record.
 */
public class LabelMarker implements Marker {

    private static final String name = "LOKI4J_LABEL_MARKER";

    private final Supplier<Map<String, String>> labelsSupplier;

    private final AtomicReference<Map<String, String>> labelsValue = new AtomicReference<>(null);

    public LabelMarker(Supplier<Map<String, String>> labelsSupplier) {
        if (labelsSupplier == null)
            throw new IllegalArgumentException("Labels can not be null");

        this.labelsSupplier = labelsSupplier;
    }

    @Override
    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        labelsValue.compareAndSet(null, labelsSupplier.get());
        return labelsValue.get();
    }

    @Override
    public void add(Marker reference) {
        throw new UnsupportedOperationException("LabelMarker does not support adding references");
    }

    @Override
    public boolean remove(Marker reference) {
        return false;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasReferences() {
        return false;
    }

    @Override
    public Iterator<Marker> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean contains(Marker other) {
        return false;
    }

    @Override
    public boolean contains(String name) {
        return false;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    /**
     * Creates a Marker containing a set of Loki labels, where each label is a key-value pair.
     * @param labels Lables will be created at time when they are first accessed (i.e. deferred creation).
     */
    public static LabelMarker of(Supplier<Map<String, String>> labels) {
        return new LabelMarker(labels);
    }

    /**
     * Creates a Marker containing one Loki label, that is a key-value pair.
     * @param key Key of the label (assumed to be static).
     * @param value Value od the label will be created at time when it is first accessed (i.e. deferred creation).
     */
    public static LabelMarker of(String key, Supplier<String> value) {
        return of(() -> Collections.singletonMap(key, value.get()));
    }
    
}
