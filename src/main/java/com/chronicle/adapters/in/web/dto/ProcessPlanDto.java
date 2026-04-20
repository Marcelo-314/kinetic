package com.chronicle.adapters.in.web.dto;

import java.util.List;

public record ProcessPlanDto(
        String sourceFolder,
        String selectionMode,
        List<String> selectedFiles,
        int totalPlannedFiles,
        int batchSize,
        String summaryPolicy,
        String failurePolicy
) {
}
