package com.github.loki4j.testkit.benchmark;

import java.util.Iterator;
import java.util.stream.StreamSupport;

public class InfiniteArrayIterator<E> implements Iterator<E> {
    private E[] es;
    private int idx = -1;
    public InfiniteArrayIterator(E[] events) {
        this.es = events;
    }
    @Override
    public boolean hasNext() {
        return true;
    }
    @Override
    public E next() {
        idx++;
        if (idx >= es.length) {
            idx = 0;
        }
        return es[idx];
    }
    public Iterator<E> limited(long limit) {
        Iterable<E> iterable = () -> this;
        return StreamSupport.stream(iterable.spliterator(), false)
            .limit(limit)
            .iterator();
    }
    public static <E> InfiniteArrayIterator<E> from(E[] sampleEvents) {
        return new InfiniteArrayIterator<>(sampleEvents);
    }
}
