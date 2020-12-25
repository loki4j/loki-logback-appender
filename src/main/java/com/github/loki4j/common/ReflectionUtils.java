package com.github.loki4j.common;

import java.util.Optional;

public class ReflectionUtils {

    /**
     * Try to create an instance of a given class by calling no-args constructor
     * @param <T> Type to converted created instance to
     * @param clazz Class name to create an instance of
     * @return Some if instance was created successfully, empty if an error occured
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> tryCreateInstance(String clazz) {
        try {
            var c = Class.forName(clazz);
            var res = (T) c.getDeclaredConstructor().newInstance();
            return Optional.of(res);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
    
}
