package com.github.loki4j.common.util;

public class StringUtils {

    /**
     * Calculate the number of bytes required to store given string
     * in UTF-8 encoding.
     */
    public static int utf8Length(CharSequence input) {
        int count = 0;
        for (int i = 0, len = input.length(); i < len; i++) {
            char ch = input.charAt(i);
            if (ch <= 0x7F) {
                count++;
            } else if (ch <= 0x7FF) {
                count += 2;
            } else if (Character.isHighSurrogate(ch)) {
                count += 4;
                ++i;
            } else {
                count += 3;
            }
        }
        return count;
    }

}
