package com.github.loki4j.testkit.dummy;

public class ExceptionGenerator {

    public static Exception exception(String message) {
        return new RuntimeException(message);
    }
    
}
