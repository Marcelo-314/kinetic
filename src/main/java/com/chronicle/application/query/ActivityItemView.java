package com.chronicle.application.query;

import java.time.Instant;
import java.util.Map;

public record ActivityItemView(
        String activityId,
        Instant timestamp,
        String eventType,
        String eventStage,
        String message,
        Map<String, Object> metadata
) {
}
