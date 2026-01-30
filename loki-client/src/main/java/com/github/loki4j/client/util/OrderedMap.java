package com.github.loki4j.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
* Standard Map.of() does not guarantee the determined order of key-value pairs in the resulting map.
* This utility class fixes this by using LinkedHashMap under the hood.
*/
public class OrderedMap {
    
    public static Map<String, String> of(String... values) {
        var result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }
}
