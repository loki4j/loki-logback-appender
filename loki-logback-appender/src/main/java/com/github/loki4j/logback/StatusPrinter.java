package com.github.loki4j.logback;

import ch.qos.logback.core.status.OnErrorConsoleStatusListener;
import ch.qos.logback.core.status.Status;

/**
 * Utility class for printing log messages from Loki4j itself to STDERR
 */
public class StatusPrinter extends OnErrorConsoleStatusListener {

    private final int minLevel;

    public StatusPrinter(int minLevel) {
        super.setRetrospective(0L);
        this.minLevel = minLevel;
    }

    @Override
    public void addStatusEvent(Status status) {
        if (status.getEffectiveLevel() >= minLevel)
            super.addStatusEvent(status);
    }

}
