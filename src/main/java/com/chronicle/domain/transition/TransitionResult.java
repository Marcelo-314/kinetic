package com.chronicle.domain.transition;

import com.chronicle.domain.command.ProcessCommand;
import com.chronicle.domain.model.ProcessAggregate;

public record TransitionResult(
        ProcessAggregate newAggregate,
        ProcessCommand command
) {

    public TransitionResult {
        if (newAggregate == null) {
            throw new IllegalArgumentException("newAggregate must not be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
    }
}
