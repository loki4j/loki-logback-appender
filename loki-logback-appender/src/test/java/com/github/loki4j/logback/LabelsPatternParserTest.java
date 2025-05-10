package com.github.loki4j.logback;

import static com.github.loki4j.logback.LabelsPatternParser.*;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class LabelsPatternParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyPattern() {
        extractKVPairsFromPattern("", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyValue() {
        extractKVPairsFromPattern("level=,app=\"my\"app", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectValue() {
        extractKVPairsFromPattern("level=%level app=\"my\"app", ",", "=");
    }

    @Test
    public void testExtractStreamKVPairsIgnoringEmpty() {
        var kvs1 = extractKVPairsFromPattern(",,level=%level,,app=\"my\"app,", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsIgnoringWhitespace() {
        var kvs1 = extractKVPairsFromPattern("\tlevel = %level,\n\tapp=\"my\"app,\n", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
                "regex:(\n|//[^\n]+)+",
                "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByNewLineRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\r\nlevel=%level\rthread=t1\napp=\"my\"app\n\r\r\r",
                "regex:\n|\r",
                "=");
        var kvse1 = Map.of("level", "%level", "thread", "t1", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairs() {
        var kvs1 = extractKVPairsFromPattern("level=%level,app=\"my\"app,test=test", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app", "test", "test");
        assertEquals("Split by ,=", kvse1, kvs1);

        var kvs2 = extractKVPairsFromPattern("level:%level;app:\"my\"app;test:test", ";", ":");
        assertEquals("Split by ;:", kvse1, kvs2);

        var kvs3 = extractKVPairsFromPattern("level.%level|app.\"my\"app|test.test", "|", ".");
        assertEquals("Split by |.", kvse1, kvs3);
    }
}
