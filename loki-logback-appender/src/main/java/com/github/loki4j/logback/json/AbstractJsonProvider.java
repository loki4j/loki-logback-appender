package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * An abstract provider that writes a certain aspect of a logging event as a JSON fragment
 */
public abstract class AbstractJsonProvider extends ContextAwareBase implements JsonProvider<ILoggingEvent> {

    private boolean enabled = true;

    private volatile boolean started;
    
    @Override
    public void start() {
        started = true;
    }
    
    @Override
    public void stop() {
        started = false;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
