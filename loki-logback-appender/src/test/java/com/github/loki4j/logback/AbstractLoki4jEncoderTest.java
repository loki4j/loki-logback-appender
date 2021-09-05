package com.github.loki4j.logback;

import org.junit.Test;

import static org.junit.Assert.*;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectFormat() {
        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", "|", "~", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |~", kvse1, kvs1);
        });
    }

    @Test
    public void testExtractStreamKVPairs() {
        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });

        withEncoder(toStringEncoder(
                labelCfg("level:%level;app:\"my\"app", ";", ":", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs2 = encoder.extractStreamKVPairs("level:INFO;app:\"my\"app;test:test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ;:", kvse1, kvs2);
        });

        withEncoder(toStringEncoder(
                labelCfg("level.%level|app.\"my\"app", "|", ".", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs3 = encoder.extractStreamKVPairs("level.INFO|app.\"my\"app|test.test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |.", kvse1, kvs3);
        });
    }

    @Test
    public void testNanoCounter() {
        var enc = toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            true,
            false);

        assertEquals(123000, enc.timestampToNanos(1123));
        assertEquals(123001, enc.timestampToNanos(1123));
        assertEquals(123002, enc.timestampToNanos(1123));
        assertEquals(123003, enc.timestampToNanos(1123));

        assertEquals(122999, enc.timestampToNanos(1122));

        assertEquals(124000, enc.timestampToNanos(1124));
        for (int i = 0; i < 997; i++)
            enc.timestampToNanos(1124);
        assertEquals(124998, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
    }
}
