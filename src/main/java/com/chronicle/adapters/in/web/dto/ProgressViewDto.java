package com.chronicle.adapters.in.web.dto;

import java.time.Instant;

public record ProgressViewDto(
        int totalFiles,
        int processedFiles,
        int successfulFiles,
        int failedFiles,
        int pendingFiles,
        double percentage,
        Instant startedAt,
        Instant estimatedCompletion,
        Instant lastProgressAt
) {
}
