package com.github.loki4j.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoftLimitBufferTest {

    @Test
    public void testOffer() {
        var buffer = new SoftLimitBuffer<Integer>(3);

        assertTrue("Offer available", buffer.offer(0));
        assertTrue("Offer available", buffer.offer(1));
        assertTrue("Offer available", buffer.offer(2));
        assertTrue("Offer unavailable", !buffer.offer(3));
        assertTrue("Offer unavailable", !buffer.offer(4));
        assertTrue("Offer unavailable", !buffer.offer(5));

        assertEquals("message", 0, (int)buffer.poll());
        assertTrue("Offer unavailable", !buffer.offer(6));

        buffer.commit(1);
        assertTrue("Offer available", buffer.offer(7));
        assertTrue("Offer unavailable", !buffer.offer(8));
    }

    @Test
    public void testPoll() {
        var buffer = new SoftLimitBuffer<Integer>(3);

        buffer.offer(0);
        buffer.offer(1);
        buffer.offer(2);

        assertEquals("size is correct", 3, buffer.size());

        assertEquals("message", 0, (int)buffer.poll());
        assertEquals("message", 1, (int)buffer.poll());
        buffer.commit(2);

        buffer.offer(3);
        buffer.offer(4);

        assertEquals("message", 2, (int)buffer.poll());
        assertEquals("message", 3, (int)buffer.poll());
    }
    
}
