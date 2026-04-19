package com.chronicle.domain.transition;

import com.chronicle.domain.command.ProcessCommand;
import com.chronicle.domain.state.ProcessState;

public final class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(ProcessState state, ProcessCommand command) {
        super("Invalid transition: state=%s, command=%s".formatted(state.code(), command.getClass().getSimpleName()));
    }
}
