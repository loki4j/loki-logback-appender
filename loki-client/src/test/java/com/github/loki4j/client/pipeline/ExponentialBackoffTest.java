package com.github.loki4j.client.pipeline;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExponentialBackoffTest {

    @Test
    public void testExponentialDelay() {
        var backoff = new ExponentialBackoff(1, 8);
        assertEquals(1, backoff.nextDelay());
        assertEquals(2, backoff.nextDelay());
        assertEquals(4, backoff.nextDelay());
        assertEquals(8, backoff.nextDelay());
        assertEquals(8, backoff.nextDelay());
        backoff.reset();
        assertEquals(1, backoff.nextDelay());
    }

    @Test
    public void testConstantDelay() {
        var backoff = new ExponentialBackoff(2, 2);
        assertEquals(2, backoff.nextDelay());
        assertEquals(2, backoff.nextDelay());
        assertEquals(2, backoff.nextDelay());
        backoff.reset();
        assertEquals(2, backoff.nextDelay());
    }
}
