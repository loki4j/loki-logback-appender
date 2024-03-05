package com.github.loki4j.logback.performance.reg_v151;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.loki4j.client.util.Cache;

/**
 * Simple cache implementation based on atomic reference on unbound hash map.
 * Cached key-values are stored forever, no eviction strategy is available.
 */
public class UnboundAtomicMapCache <K, V> implements Cache<K, V> {
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
