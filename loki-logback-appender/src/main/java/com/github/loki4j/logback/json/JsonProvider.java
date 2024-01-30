package com.github.loki4j.logback.json;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * A provider that writes a certain aspect of a logging event to a JSON
 */
public interface JsonProvider<Event extends DeferredProcessingAware> extends ContextAware, LifeCycle {

    /**
     * Indicates if this provider is enabled.
     * For a disabled provider no other its methods should be called.
     */
    boolean isEnabled();

    /**
     * Allows to configure if the provider is enabled.
     */
    void setEnabled(boolean enabled);

    /**
     * Indicates if this provider can write anything for a particular event.
     * If this method returns {@code false}, {@code writeTo()} should not be called for a particular event.
     * You can put all your preliminary checks here, no need to duplicated them in {@code writeTo()}.
     */
    boolean canWrite(Event event);
    
    /**
     * Writes a certain aspect of event into a writer.
     * @param writer JSON writer to use.
     * @param event Current logback event.
     * @param startWithSeparator If {@code true}, a separator should be written before writing anything else.
     * @return If anything was effectively written during this call.
     */
    boolean writeTo(JsonEventWriter writer, Event event, boolean startWithSeparator);
}
