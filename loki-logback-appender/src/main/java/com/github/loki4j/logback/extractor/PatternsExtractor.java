package com.github.loki4j.logback.extractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
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
public class PatternsExtractor implements Extractor {

    private static final int INITIAL_STRING_BUILDER_SIZE = 64;

    private final List<String> keys;
    private final List<Converter<ILoggingEvent>> converters = new ArrayList<>();

    public PatternsExtractor(Map<String, String> patterns, Context context) throws ScanException {
        keys = List.copyOf(patterns.keySet());

        var patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        var effectiveConverterMap = patternLayout.getEffectiveConverterMap();

        for (var pattern : patterns.values()) {
            try {
                Parser<ILoggingEvent> p = new Parser<>(pattern);
                if (context != null)
                    p.setContext(context);
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

    public void extract(ILoggingEvent event, Map<String, String> result) {
        StringBuilder strBuilder = new StringBuilder(INITIAL_STRING_BUILDER_SIZE);
        for (var i = 0; i < converters.size(); i++) {
            var c = converters.get(i);
            while (c != null) {
                c.write(strBuilder, event);
                c = c.getNext();
            }
            result.put(keys.get(i), strBuilder.toString());
            strBuilder.setLength(0);
        }
    }
}
