package com.github.loki4j.logback.performance.reg_v110;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ConcurrentBatchBuffer<I, E> {
    
    private final Object[] items;

    private int index;

    private final ReentrantLock lock;

    private long lastBatchTime;

    private Supplier<E> factory;
    private BiFunction<I, E, E> transformer;

    public ConcurrentBatchBuffer(int capacity, Supplier<E> factory, BiFunction<I, E, E> transformer) {
        if (capacity < 1)
            throw new IllegalArgumentException("The capacity of the buffer should be at least 1 element");

        this.index = 0;
        this.lastBatchTime = System.currentTimeMillis();
        this.factory = factory;
        this.transformer = transformer;

        this.lock = new ReentrantLock(false);
        this.items = new Object[capacity];
        initItems();
    }

    @SuppressWarnings("unchecked")
    public E[] add(I input, E[] zeroSizeArray) {
        Objects.requireNonNull(input);
        E[] batch = zeroSizeArray;

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final Object[] items = this.items;
            items[index] = Objects.requireNonNull(transformer.apply(input, (E)items[index]));
            if (++index == items.length) {
                batch = (E[]) Arrays.copyOf(items, items.length, zeroSizeArray.getClass());
                index = 0;
                lastBatchTime = System.currentTimeMillis();
                initItems();
            }
        } finally {
            lock.unlock();
        }

        return batch;
    }

    @SuppressWarnings("unchecked")
    public E[] drain(long timeoutMs, E[] zeroSizeArray) {
        E[] batch = zeroSizeArray;

        final long now = System.currentTimeMillis();
        final ReentrantLock lock = this.lock;
        lock.lock();
        if (index > 0 && now - lastBatchTime > timeoutMs) {
            batch = (E[]) Arrays.copyOf(items, index, zeroSizeArray.getClass());
            index = 0;
            lastBatchTime = System.currentTimeMillis();
            initItems();
        }
        lock.unlock();

        return batch;
    }

    public int getCapacity() {
        return items.length;
    }

    private void initItems() {
        for (int i = 0; i < items.length; i++) {
            items[i] = factory.get();
        }
    }

}
