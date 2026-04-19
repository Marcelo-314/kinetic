package com.chronicle.domain.transition;

import com.chronicle.domain.command.AuthorizeProcess;
import com.chronicle.domain.command.PauseProcess;
import com.chronicle.domain.command.ProcessCommand;
import com.chronicle.domain.command.ResolveCheckpoint;
import com.chronicle.domain.command.ResumeProcess;
import com.chronicle.domain.command.StopProcess;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.state.Completed;
import com.chronicle.domain.state.Failed;
import com.chronicle.domain.state.Paused;
import com.chronicle.domain.state.Pending;
import com.chronicle.domain.state.ProcessState;
import com.chronicle.domain.state.Running;
import com.chronicle.domain.state.Stopped;

public final class DefaultTransitionEngine implements TransitionEngine {

    @Override
    public TransitionResult apply(ProcessAggregate process, ProcessCommand command, TransitionContext context) {
        if (process == null) {
            throw new IllegalArgumentException("process must not be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        ProcessAggregate newAggregate = switch (process.state()) {
            case Pending ignored -> applyPending(process, command);
            case Running ignored -> applyRunning(process, command, context);
            case Paused ignored -> applyPaused(process, command);
            case Completed ignored -> invalid(process.state(), command);
            case Failed ignored -> invalid(process.state(), command);
            case Stopped ignored -> invalid(process.state(), command);
        };

        return new TransitionResult(newAggregate, command);
    }

    private ProcessAggregate applyPending(ProcessAggregate process, ProcessCommand command) {
        return switch (command) {
            case AuthorizeProcess ignored -> process.transitionTo(ProcessState.running(), false, false);
            case StopProcess ignored -> process.transitionTo(ProcessState.stopped(), false, false);
            default -> invalid(process.state(), command);
        };
    }

    private ProcessAggregate applyRunning(ProcessAggregate process, ProcessCommand command, TransitionContext context) {
        return switch (command) {
            case PauseProcess ignored -> process.transitionTo(process.state(), true, process.stopRequested());
            case StopProcess ignored -> process.transitionTo(process.state(), process.pauseRequested(), true);
            case ResolveCheckpoint ignored -> resolveRunningCheckpoint(process, context);
            default -> invalid(process.state(), command);
        };
    }

    private ProcessAggregate applyPaused(ProcessAggregate process, ProcessCommand command) {
        return switch (command) {
            case ResumeProcess ignored -> process.transitionTo(ProcessState.running(), false, false);
            case StopProcess ignored -> process.transitionTo(ProcessState.stopped(), false, false);
            default -> invalid(process.state(), command);
        };
    }

    private ProcessAggregate resolveRunningCheckpoint(ProcessAggregate process, TransitionContext context) {
        if (context.failureDetected()) {
            return process.transitionTo(ProcessState.failed(), false, false);
        }
        if (process.stopRequested()) {
            return process.transitionTo(ProcessState.stopped(), false, false);
        }
        if (context.workCompleted()) {
            return process.transitionTo(ProcessState.completed(), false, false);
        }
        if (process.pauseRequested()) {
            return process.transitionTo(ProcessState.paused(), false, false);
        }
        return process;
    }

    private ProcessAggregate invalid(ProcessState state, ProcessCommand command) {
        throw new InvalidTransitionException(state, command);
    }
}
