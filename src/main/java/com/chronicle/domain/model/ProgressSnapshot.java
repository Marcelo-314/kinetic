package com.chronicle.domain.model;

import java.time.Instant;

public record ProgressSnapshot(
        String progressSnapshotId,
        String processId,
        int totalFiles,
        int processedFiles,
        int successfulFiles,
        int failedFiles,
        int pendingFiles,
        double percentage,
        Integer currentBatchIndex,
        Integer currentBatchSize,
        Instant startedAt,
        Instant estimatedCompletion,
        Instant lastProgressAt
) {
}
