package com.github.loki4j.logback;

import static com.github.loki4j.logback.LabelsPatternParser.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.loki4j.client.util.OrderedMap;

public class LabelsPatternParserTest {

    @Test
    public void testExtractStreamKVPairsEmptyPattern() {
        assertThrows(IllegalArgumentException.class,
                () -> extractKVPairsFromPattern("", ",", "="));
    }

    @Test
    public void testExtractStreamKVPairsEmptyValue() {
        assertThrows(IllegalArgumentException.class,
                () -> extractKVPairsFromPattern("level=,app=\"my\"app", ",", "="));
    }

    @Test
    public void testExtractStreamKVPairsIncorrectValue() {
        assertThrows(IllegalArgumentException.class,
                () -> extractKVPairsFromPattern("level=%level app=\"my\"app", ",", "="));
        ;
    }

    @Test
    public void testExtractStreamKVPairsIgnoringEmpty() {
        var kvs1 = extractKVPairsFromPattern(",,level=%level,,app=\"my\"app,", ",", "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals(kvse1, kvs1, "Split by ,=");
    }

    @Test
    public void testExtractStreamKVPairsIgnoringWhitespace() {
        var kvs1 = extractKVPairsFromPattern("\tlevel = %level,\n\tapp=\"my\"app,\n", ",", "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals(kvse1, kvs1, "Split by ,=");
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
                "regex:(\n|//[^\n]+)+",
                "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals(kvse1, kvs1, "Split by ,=");
    }

    @Test
    public void testExtractStreamKVPairsByNewLineRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\r\nlevel=%level\rthread=t1\napp=\"my\"app\n\r\r\r",
                "regex:\n|\r",
                "=");
        var kvse1 = OrderedMap.of("level", "%level", "thread", "t1", "app", "\"my\"app").entrySet().stream().collect(toList());
        assertEquals(kvse1, kvs1, "Split by ,=");
    }

    @Test
    public void testExtractStreamKVPairs() {
        var kvs1 = extractKVPairsFromPattern("level=%level,app=\"my\"app,test=test", ",", "=");
        var kvse1 = OrderedMap.of("level", "%level", "app", "\"my\"app", "test", "test").entrySet().stream().collect(toList());
        assertEquals(kvse1, kvs1, "Split by ,=");

        var kvs2 = extractKVPairsFromPattern("level:%level;app:\"my\"app;test:test", ";", ":");
        assertEquals(kvse1, kvs2, "Split by ;:");

        var kvs3 = extractKVPairsFromPattern("level.%level|app.\"my\"app|test.test", "|", ".");
        assertEquals(kvse1, kvs3, "Split by |.");
    }

    @Test
    public void testExtractMultipleBulkPatterns() {
        var kvs1 = extractKVPairsFromPattern("level=%level,*=%%mdc{vendor},*=%%kvp", ",", "=");
        var kvse1 = List.of(Map.entry("level", "%level"), Map.entry("*", "%%mdc{vendor}"), Map.entry("*", "%%kvp"));
        assertEquals(kvse1, kvs1, "Split by ,=");
    }

    @Test
    public void testParseBulkPattern() {
        var p = parseBulkPattern("mdc_*", "%%mdc");
        assertEquals("mdc_", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of(), p.include, "include");
        assertEquals(Set.of(), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternNoPrefix() {
        var p = parseBulkPattern("*", "%%mdc");
        assertEquals("", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of(), p.include, "include");
        assertEquals(Set.of(), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternInclude() {
        var p = parseBulkPattern("*", "%%mdc{key1, key2}");
        assertEquals("", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of("key1", "key2"), p.include, "include");
        assertEquals(Set.of(), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternExclude() {
        var p = parseBulkPattern("mdc_*!", "%%mdc{key1,key2}");
        assertEquals("mdc_", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of(), p.include, "include");
        assertEquals(Set.of("key1", "key2"), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternEmptyParams() {
        var p = parseBulkPattern("* !", "%%mdc{ }");
        assertEquals("", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of(), p.include, "include");
        assertEquals(Set.of(), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternOneParam() {
        var p = parseBulkPattern(" * ", "%%mdc { key1 }");
        assertEquals("", p.prefix, "prefix");
        assertEquals("mdc", p.func, "func");
        assertEquals(Set.of("key1"), p.include, "include");
        assertEquals(Set.of(), p.exclude, "exclude");
    }

    @Test
    public void testParseBulkPatternFailNoStar() {
        assertThrows(IllegalArgumentException.class,
                () -> parseBulkPattern("mdc_", "%%mdc{key1,key2}"));
    }

    @Test
    public void testParseBulkPatternFailNoDoublePercent() {
        assertThrows(IllegalArgumentException.class,
                () -> parseBulkPattern("mdc_", "%mdc{key1,key2}"));
    }

    @Test
    public void testParseBulkPatternFailBracesMalformed() {
        assertThrows(IllegalArgumentException.class,
                () -> parseBulkPattern("mdc_", "%mdc{key1},key2"));
    }

}
