package com.chronicle.application.exception;

public final class ProcessNotFoundException extends RuntimeException {

    public ProcessNotFoundException(String processId) {
        super("Process not found: %s".formatted(processId));
    }
}
