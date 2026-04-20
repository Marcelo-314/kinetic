package com.chronicle.application.exception;

import java.util.Map;

public class SemanticValidationException extends RuntimeException {

    private final Map<String, Object> details;

    public SemanticValidationException(String message) {
        this(message, Map.of());
    }

    public SemanticValidationException(String message, Map<String, Object> details) {
        super(message);
        this.details = Map.copyOf(details);
    }

    public Map<String, Object> details() {
        return details;
    }
}
