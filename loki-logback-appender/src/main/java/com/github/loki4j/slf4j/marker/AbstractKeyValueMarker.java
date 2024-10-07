package com.github.loki4j.slf4j.marker;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Marker;

/*
 * An abstract SLF4J marker carrying a set of key-value pairs
 */
public abstract class AbstractKeyValueMarker implements Marker {

    private final Supplier<Map<String, String>> kvSupplier;

    private final AtomicReference<Map<String, String>> kvValue = new AtomicReference<>(null);

    public AbstractKeyValueMarker(Supplier<Map<String, String>> kvSupplier) {
        if (kvSupplier == null)
            throw new IllegalArgumentException("Labels can not be null");

        this.kvSupplier = kvSupplier;
    }

    public Map<String, String> getKeyValuePairs() {
        kvValue.compareAndSet(null, kvSupplier.get());
        return kvValue.get();
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
}
