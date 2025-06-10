package com.github.loki4j.logback;

import static com.github.loki4j.logback.LabelsPatternParser.*;
import static org.junit.Assert.assertEquals;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.github.loki4j.client.util.OrderedMap;

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
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsIgnoringWhitespace() {
        var kvs1 = extractKVPairsFromPattern("\tlevel = %level,\n\tapp=\"my\"app,\n", ",", "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
                "regex:(\n|//[^\n]+)+",
                "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByNewLineRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\r\nlevel=%level\rthread=t1\napp=\"my\"app\n\r\r\r",
                "regex:\n|\r",
                "=");
        var kvse1 = OrderedMap.of("level", "%level", "thread", "t1", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairs() {
        var kvs1 = extractKVPairsFromPattern("level=%level,app=\"my\"app,test=test", ",", "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app", "test", "test").entrySet().stream().collect(toList());
        assertEquals("Split by ,=", kvse1, kvs1);

        var kvs2 = extractKVPairsFromPattern("level:%level;app:\"my\"app;test:test", ";", ":");
        assertEquals("Split by ;:", kvse1, kvs2);

        var kvs3 = extractKVPairsFromPattern("level.%level|app.\"my\"app|test.test", "|", ".");
        assertEquals("Split by |.", kvse1, kvs3);
    }

    @Test
    public void testExtractMultipleBulkPatterns() {
        var kvs1 = extractKVPairsFromPattern("level=%level,*=%%mdc{vendor},*=%%kvp", ",", "=");
        var kvse1 = List.of(Map.entry("level", "%level"), Map.entry("*", "%%mdc{vendor}"), Map.entry("*", "%%kvp"));
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testParseBulkPattern() {
        var p = parseBulkPattern("mdc_*", "%%mdc");
        assertEquals("prefix", "mdc_", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of(), p.include);
        assertEquals("exclude", Set.of(), p.exclude);
    }

    @Test
    public void testParseBulkPatternNoPrefix() {
        var p = parseBulkPattern("*", "%%mdc");
        assertEquals("prefix", "", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of(), p.include);
        assertEquals("exclude", Set.of(), p.exclude);
    }

    @Test
    public void testParseBulkPatternInclude() {
        var p = parseBulkPattern("*", "%%mdc{key1, key2}");
        assertEquals("prefix", "", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of("key1", "key2"), p.include);
        assertEquals("exclude", Set.of(), p.exclude);
    }

    @Test
    public void testParseBulkPatternExclude() {
        var p = parseBulkPattern("mdc_*!", "%%mdc{key1,key2}");
        assertEquals("prefix", "mdc_", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of(), p.include);
        assertEquals("exclude", Set.of("key1", "key2"), p.exclude);
    }

    @Test
    public void testParseBulkPatternEmptyParams() {
        var p = parseBulkPattern("* !", "%%mdc{ }");
        assertEquals("prefix", "", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of(), p.include);
        assertEquals("exclude", Set.of(), p.exclude);
    }

    @Test
    public void testParseBulkPatternOneParam() {
        var p = parseBulkPattern(" * ", "%%mdc { key1 }");
        assertEquals("prefix", "", p.prefix);
        assertEquals("func", "mdc", p.func);
        assertEquals("include", Set.of("key1"), p.include);
        assertEquals("exclude", Set.of(), p.exclude);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBulkPatternFailNoStar() {
        parseBulkPattern("mdc_", "%%mdc{key1,key2}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBulkPatternFailNoDoublePercent() {
        parseBulkPattern("mdc_", "%mdc{key1,key2}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBulkPatternFailBracesMalformed() {
        parseBulkPattern("mdc_", "%mdc{key1},key2");
    }

}
