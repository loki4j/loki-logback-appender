package com.github.loki4j.common;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public final class SoftLimitBuffer<E> extends AbstractQueue<E> {

    private final int softLimit;

    private final AtomicInteger size = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<E> items = new ConcurrentLinkedQueue<>();

    private volatile Thread waiter = null;

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
        var s = size.addAndGet(-count);
        System.out.println(s + " - " + count);
        if (s == 0 && waiter != null) LockSupport.unpark(waiter);
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
    

    public void waitForEmpty(long timeoutMs) {
        if (waiter != null && waiter != Thread.currentThread())
            throw new IllegalStateException("Waiter is already set: " + waiter.getName());
        waiter = Thread.currentThread();
        var sleepMs = 10L;
        var sleepNs = TimeUnit.MILLISECONDS.toNanos(sleepMs);
        var elapsedMs = 0L;
        while(size.get() > 1000  && elapsedMs < timeoutMs) {
            LockSupport.parkNanos(this, sleepNs);
            elapsedMs += sleepMs;
        }
        if (elapsedMs >= timeoutMs)
                throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }
}
