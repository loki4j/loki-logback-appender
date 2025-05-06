package com.github.loki4j.logback.extractor;

import java.util.Map;

import com.github.loki4j.slf4j.marker.AbstractKeyValueMarker;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A component for extracting key-value pairs from SLF4J markers.
 */
public class MarkerExtractor<T extends AbstractKeyValueMarker> implements Extractor {

    private final Class<T> clazz;

    public MarkerExtractor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void extract(ILoggingEvent event, Map<String, String> result) {
        var markers = event.getMarkerList();
        if (markers == null || markers.isEmpty())
            return;

        for (var marker : markers) {
            if (marker == null || !clazz.isAssignableFrom(marker.getClass()))
                continue;

            var kvMarker = (AbstractKeyValueMarker) marker;
            result.putAll(kvMarker.getKeyValuePairs());
            return; // only one Marker is supported per event
        }

    }
}
