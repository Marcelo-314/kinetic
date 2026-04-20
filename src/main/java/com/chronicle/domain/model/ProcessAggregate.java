package com.chronicle.domain.model;

import com.chronicle.domain.state.ProcessState;

import java.time.Instant;

public record ProcessAggregate(
        String processId,
        ProcessState state,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String objective,
        ResultKind resultKind,
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
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective must not be blank");
        }
        if (resultKind == null) {
            throw new IllegalArgumentException("resultKind must not be null");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static ProcessAggregate pending(String processId, String objective, Instant now) {
        return new ProcessAggregate(processId, ProcessState.pending(), 1L, now, now, objective, ResultKind.NONE, false, false);
    }

    public static ProcessAggregate pending(String processId) {
        return pending(processId, "Test objective", Instant.EPOCH);
    }

    public static ProcessAggregate rehydrate(
            String processId,
            ProcessState state,
            long version,
            boolean pauseRequested,
            boolean stopRequested
    ) {
        return new ProcessAggregate(
                processId,
                state,
                version,
                Instant.EPOCH,
                Instant.EPOCH,
                "Test objective",
                ResultKind.NONE,
                pauseRequested,
                stopRequested
        );
    }

    public ProcessAggregate withState(ProcessState newState) {
        return new ProcessAggregate(processId, newState, version + 1, createdAt, updatedAt, objective, resultKind, pauseRequested, stopRequested);
    }

    public ProcessAggregate withControlFlags(boolean newPauseRequested, boolean newStopRequested) {
        return new ProcessAggregate(processId, state, version + 1, createdAt, updatedAt, objective, resultKind, newPauseRequested, newStopRequested);
    }

    public ProcessAggregate clearControlFlags() {
        return new ProcessAggregate(processId, state, version + 1, createdAt, updatedAt, objective, resultKind, false, false);
    }

    public ProcessAggregate transitionTo(ProcessState newState, boolean newPauseRequested, boolean newStopRequested) {
        return new ProcessAggregate(processId, newState, version + 1, createdAt, updatedAt, objective, resultKind, newPauseRequested, newStopRequested);
    }

    public ProcessAggregate touch(Instant newUpdatedAt) {
        return new ProcessAggregate(processId, state, version, createdAt, newUpdatedAt, objective, resultKind, pauseRequested, stopRequested);
    }

    public ProcessAggregate withResultKind(ResultKind newResultKind, Instant newUpdatedAt) {
        return new ProcessAggregate(processId, state, version, createdAt, newUpdatedAt, objective, newResultKind, pauseRequested, stopRequested);
    }
}
