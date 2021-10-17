package com.github.loki4j.client.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static java.nio.charset.StandardCharsets.UTF_8;;

public class StringUtilsTest {

    @Test
    public void testUtf8Length() {
        for (int codepoint = Character.MIN_CODE_POINT; codepoint <= Character.MAX_CODE_POINT; codepoint++) {
            if(codepoint == Character.MIN_SURROGATE) codepoint=Character.MAX_SURROGATE + 1;
            if(!Character.isDefined(codepoint)) continue;
            String test = new String(Character.toChars(codepoint));
            assertEquals(test.getBytes(UTF_8).length, StringUtils.utf8Length(test));
        }
    }
    
}
