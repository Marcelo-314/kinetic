package com.chronicle.application.query;

import com.chronicle.domain.model.ResultKind;

import java.time.Instant;

public record ProcessListItemView(
        String processId,
        String status,
        Instant createdAt,
        Instant updatedAt,
        ResultKind resultKind,
        int totalFiles,
        int processedFiles,
        double percentage
) {
}
