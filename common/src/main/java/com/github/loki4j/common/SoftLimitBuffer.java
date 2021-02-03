package com.github.loki4j.common;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SoftLimitBuffer<E> extends AbstractQueue<E> {

    private final int softLimit;

    private final AtomicInteger size = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<E> items = new ConcurrentLinkedQueue<>();

    public SoftLimitBuffer(int softLimit) {
        this.softLimit = softLimit;
    }

    public boolean offer(Supplier<E> e) {
        if (size.get() + 1 > softLimit)
            return false;
        
        items.offer(e.get());
        size.incrementAndGet();
        return true;
    }

    public void commit(int count) {
        size.addAndGet(-count);
    }

    @Override
    public boolean offer(E e) {
        return offer(() -> e);
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public E poll() {
        return items.poll();
    }

    @Override
    public E peek() {
        return items.peek();
    }

    @Override
    public Iterator<E> iterator() {
        return items.iterator();
    }
    
}
