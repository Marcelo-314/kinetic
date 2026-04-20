package com.chronicle.domain.transition;

import com.chronicle.domain.command.ProcessCommand;
import com.chronicle.domain.model.ProcessAggregate;

public interface TransitionEngine {

    TransitionResult apply(ProcessAggregate process, ProcessCommand command, TransitionContext context);
}
