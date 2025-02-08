package com.github.loki4j.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;

/**
 * A component for rendering several Logback patters at a time.
 * Used for effective label/metadata extraction.
 */
public class LogbackPatternsExtractor<E> {

    private static final int INITIAL_STRING_BUILDER_SIZE = 64;

    private final List<Converter<E>> converters = new ArrayList<>();

    public LogbackPatternsExtractor(List<String> patterns, Context context) throws ScanException {
        var patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        var effectiveConverterMap = patternLayout.getEffectiveConverterMap();

        for (var pattern: patterns) {
            try {
                Parser<E> p = new Parser<E>(pattern);
                if (context != null) p.setContext(context);
                Node t = p.parse();
                var converter = p.compile(t, effectiveConverterMap);
                ConverterUtil.setContextForConverters(context, converter);
                ConverterUtil.startConverters(converter);
                converters.add(converter);
            } catch (ScanException sce) {
                throw new ScanException("Unable to parse pattern: \"" + pattern + "\"", sce);
            }
        }
    }

    public String[] extract(E event) {
        StringBuilder strBuilder = new StringBuilder(INITIAL_STRING_BUILDER_SIZE);
        var result = new String[converters.size()];
        for (var i = 0; i < converters.size(); i++) {
            var c = converters.get(i);
            while (c != null) {
                c.write(strBuilder, event);
                c = c.getNext();
            }
            result[i] = strBuilder.toString();
            strBuilder.setLength(0);
        }
        return result;
    }
}
