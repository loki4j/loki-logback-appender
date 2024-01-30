package com.github.loki4j.pkg.dslplatform.json;

public class NumberConverter {

    private static final byte MINUS = '-';
    private static final byte ZERO = '0';
    private static final byte[] DIGITS = new byte[40];

    public static void serialize(final long value, final RawJsonWriter sw) {
        var num = value;
        if (num == 0) {
            sw.writeByte(ZERO);
            return;
        }
        if (num < 0) {
            sw.writeByte(MINUS);
            num = -num;
        }
        var pos = 0;
        while (num != 0) {
            DIGITS[pos++] = (byte) (num % 10);
            num = num / 10;
        }
        while (pos > 0) {
            byte digit = (byte) (ZERO + DIGITS[--pos]);
            sw.writeByte(digit);
        }
    }
}
