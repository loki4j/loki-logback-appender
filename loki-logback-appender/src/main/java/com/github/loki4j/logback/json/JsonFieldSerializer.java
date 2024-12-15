package com.github.loki4j.logback.json;

/**
 * This interface allows to customize how a particular field is written to JSON.
 */
@FunctionalInterface
public interface JsonFieldSerializer {

    /**
     * Write a field into JSON event layout.
     * @param writer JSON writer to use.
     * @param fieldName Name of the field to write.
     * @param fieldValue Value of the field to write.
     */
    void writeField(JsonEventWriter writer, String fieldName, Object fieldValue);

}
