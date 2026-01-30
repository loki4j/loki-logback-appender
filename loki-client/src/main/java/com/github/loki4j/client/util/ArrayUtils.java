package com.github.loki4j.client.util;

import java.util.Arrays;

public class ArrayUtils {
    
    public static String[] concat(String[] arr1, String[] arr2) {
        if (arr2.length == 0) return arr1;
        if (arr1.length == 0) return arr2;

        var result = Arrays.copyOf(arr1, arr1.length + arr2.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
        return result;
    }

    public static String join2(String[] arr1, String[] arr2, String separator) {
        var sb = new StringBuilder(32 * (arr1.length + arr2.length));
        for (var str: arr1) {
            sb.append(str);
            sb.append(separator);
        }
        for (var str: arr2) {
            sb.append(str);
            sb.append(separator);
        }
        return sb.toString();
    }
}
