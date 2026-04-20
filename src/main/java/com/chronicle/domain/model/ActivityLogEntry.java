package com.chronicle.domain.model;

import java.time.Instant;
import java.util.Map;

public record ActivityLogEntry(
        String activityId,
        String processId,
        Instant timestamp,
        String eventType,
        String eventStage,
        String message,
        Map<String, Object> metadata,
        String correlationId
) {

    public ActivityLogEntry {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
