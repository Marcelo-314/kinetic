package com.chronicle.adapters.in.web.dto;

import java.time.Instant;
import java.util.Map;

public record ActivityItemDto(
        String activityId,
        Instant timestamp,
        String eventType,
        String eventStage,
        String message,
        Map<String, Object> metadata
) {
}
