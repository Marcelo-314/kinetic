package com.chronicle.domain.model;

import com.chronicle.domain.state.ProcessState;

public record ProcessAggregate(
        String processId,
        ProcessState state,
        long version,
        boolean pauseRequested,
        boolean stopRequested
) {

    public ProcessAggregate {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("processId must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than or equal to one");
        }
    }

    public static ProcessAggregate pending(String processId) {
        return new ProcessAggregate(processId, ProcessState.pending(), 1L, false, false);
    }

    public ProcessAggregate withState(ProcessState newState) {
        return new ProcessAggregate(processId, newState, version + 1, pauseRequested, stopRequested);
    }

    public ProcessAggregate withControlFlags(boolean newPauseRequested, boolean newStopRequested) {
        return new ProcessAggregate(processId, state, version + 1, newPauseRequested, newStopRequested);
    }

    public ProcessAggregate clearControlFlags() {
        return new ProcessAggregate(processId, state, version + 1, false, false);
    }

    public ProcessAggregate transitionTo(ProcessState newState, boolean newPauseRequested, boolean newStopRequested) {
        return new ProcessAggregate(processId, newState, version + 1, newPauseRequested, newStopRequested);
    }
}
