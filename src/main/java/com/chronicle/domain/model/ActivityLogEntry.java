package com.chronicle.domain.model;

import java.time.Instant;

public record ActivityLogEntry(
        String activityId,
        String processId,
        Instant timestamp,
        String eventType,
        String eventStage,
        String message
) {
}
