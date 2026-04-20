package com.chronicle.adapters.in.web.dto;

import java.time.Instant;

public record ProcessListItemDto(
        String processId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String resultKind,
        ProcessListProgressDto progress
) {
}
