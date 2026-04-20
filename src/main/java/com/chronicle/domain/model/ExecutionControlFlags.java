package com.chronicle.domain.model;

import java.time.Instant;

public record ExecutionControlFlags(
        String processId,
        boolean pauseRequested,
        boolean stopRequested,
        Instant lastControlCommandAt,
        String lastControlCommandType
) {
}
