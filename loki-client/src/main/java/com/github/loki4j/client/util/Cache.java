package com.github.loki4j.client.util;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A simple cache interface.
 */
public interface Cache<K, V> {

    /**
     * Get the value from cache based by key.
     * @param key Key to get a value from the cache 
     * @param onMiss Supplier for a value in case it's missing in cache
     */
    V get(K key, Supplier<V> onMiss);

    /**
     * Simple cache implementation based on atomic reference on unbound hash map.
     */
    public static class UnboundAtomicMapCache<K, V> implements Cache<K, V> {
        private final AtomicReference<HashMap<K, V>> cache = new AtomicReference<>(new HashMap<>());

        @Override
        public V get(K key, Supplier<V> onMiss) {
            return cache
                .updateAndGet(m -> {
                    if (!m.containsKey(key)) {
                        var nm = new HashMap<>(m);
                        nm.put(key, onMiss.get());
                        return nm;
                    } else {
                        return m;
                    }
                })
                .get(key);
        }
    }
}
