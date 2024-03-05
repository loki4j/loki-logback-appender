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
     * Simple cache implementation based on atomic reference on bound hash map.
     * Cache grows up to the given limit. When it reaches the limit, it's being truncated to zero.
     * Then it starts the next cycle of accumulating key-values until the limit is reached again.
     */
    public static class BoundAtomicMapCache<K, V> implements Cache<K, V> {
        private final AtomicReference<HashMap<K, V>> cache = new AtomicReference<>(new HashMap<>());

        private int maxCachedItems = 1000;

        @Override
        public V get(K key, Supplier<V> onMiss) {
            return cache
                .updateAndGet(m -> {
                    if (!m.containsKey(key)) {
                        var nm = m.size() < maxCachedItems
                            ? new HashMap<K, V>(m)      // keeping values until the limit is reached
                            : new HashMap<K, V>();      // truncate the cache if the limit is reached
                        nm.put(key, onMiss.get());
                        return nm;
                    } else {
                        return m;
                    }
                })
                .get(key);
        }

        public void setMaxCachedItems(int maxCachedItems) {
            this.maxCachedItems = maxCachedItems;
        }
    }
}
