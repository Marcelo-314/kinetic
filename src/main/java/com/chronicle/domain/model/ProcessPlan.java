package com.chronicle.domain.model;

import java.time.Instant;
import java.util.List;

public record ProcessPlan(
        String planId,
        String processId,
        String sourceFolder,
        SelectionMode selectionMode,
        List<String> selectedFiles,
        int totalPlannedFiles,
        int batchSize,
        SummaryPolicy summaryPolicy,
        FailurePolicy failurePolicy,
        Instant createdAt
) {

    public ProcessPlan {
        selectedFiles = List.copyOf(selectedFiles);
    }
}
