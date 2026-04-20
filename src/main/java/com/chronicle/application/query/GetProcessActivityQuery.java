package com.chronicle.application.query;

import java.time.Instant;

public record GetProcessActivityQuery(
        String processId,
        Instant from,
        Instant to,
        String eventType,
        int page,
        int pageSize
) {
}
