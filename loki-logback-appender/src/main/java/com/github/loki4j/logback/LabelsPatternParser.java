package com.github.loki4j.logback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.loki4j.client.util.StringUtils;

/**
 * Utility class for parsing key-value pairs from label and structured metadata patterns
 * defined by user in appender's config.
 */
public class LabelsPatternParser {

    public static final String KV_REGEX_STARTER = "regex:";

    public static final Pattern BULK_PATTERN_KEY_REGEX = Pattern.compile("^(.*)\\*[ ]*(!)?$");
    public static final Pattern BULK_PATTERN_VALUE_REGEX = Pattern.compile("^%%([^{]+)(\\{([^}]*)\\})?$");

    public static List<Entry<String, String>> extractKVPairsFromPattern(String pattern, String pairSeparator, String keyValueSeparator) {
        // check if label pair separator is RegEx or literal string
        var pairSeparatorPattern = pairSeparator.startsWith(KV_REGEX_STARTER)
            ? Pattern.compile(pairSeparator.substring(KV_REGEX_STARTER.length()))
            : Pattern.compile(Pattern.quote(pairSeparator));
        // label key-value separator supports only literal strings
        var keyValueSeparatorPattern = Pattern.compile(Pattern.quote(keyValueSeparator));

        var pairs = pairSeparatorPattern.split(pattern);
        var result = new ArrayList<Entry<String, String>>();
        for (int i = 0; i < pairs.length; i++) {
            if (StringUtils.isBlank(pairs[i])) continue;

            var kv = keyValueSeparatorPattern.split(pairs[i]);
            if (kv.length == 2) {
                result.add(Map.entry(kv[0].trim(), kv[1].trim()));
            } else {
                throw new IllegalArgumentException(String.format(
                    "Unable to split '%s' in '%s' to key-value pairs, pairSeparator=%s, keyValueSeparator=%s",
                    pairs[i], pattern, pairSeparator, keyValueSeparator));
            }
        }
        if (result.isEmpty())
            throw new IllegalArgumentException("Empty of blank patterns are not supported");
        return result;
    }

    public static boolean isBulkPattern(Entry<String, String> kvp) {
        return kvp.getValue().startsWith("%%") && kvp.getKey().contains("*");   // TODO: switch to strict regex check?
    }

    public static BulkPattern parseBulkPattern(String key, String value) {
        var keyMatch = BULK_PATTERN_KEY_REGEX.matcher(key);
        if (!keyMatch.find())
            throw new IllegalArgumentException("Unable to parse bulk pattern key: " + key);
        var prefix = keyMatch.group(1).trim();
        var neg = keyMatch.group(2) != null;

        var valueMatch = BULK_PATTERN_VALUE_REGEX.matcher(value);
        if (!valueMatch.find())
            throw new IllegalArgumentException("Unable to parse bulk pattern value: " + value);
        var func = valueMatch.group(1).trim();
        var paramsStr = valueMatch.group(3);
        var params = paramsStr != null && !paramsStr.isBlank()
            ? Arrays.stream(valueMatch.group(3).split(",")).map(String::trim).collect(Collectors.toSet())
            : Set.<String>of();
        var include = !neg ? params : Set.<String>of();
        var exclude = neg ? params : Set.<String>of();

        return new BulkPattern(prefix, func, include, exclude);
    }


    public static class BulkPattern {
        public String prefix;
        public String func;
        public Set<String> include;
        public Set<String> exclude;
        public BulkPattern(String prefix, String func, Set<String> include, Set<String> exclude) {
            this.prefix = prefix;
            this.func = func;
            this.include = include;
            this.exclude = exclude;
        }
        @Override
        public String toString() {
            return "BulkPattern [prefix=" + prefix + ", func=" + func + ", include=" + include + ", exclude=" + exclude + "]";
        }
    }
}
