package com.chronicle.application.request;

import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;

import java.util.List;

public record CreateProcessRequest(
        String objective,
        String sourceFolder,
        SelectionMode selectionMode,
        List<String> selectedFiles,
        int batchSize,
        SummaryPolicy summaryPolicy,
        FailurePolicy failurePolicy,
        boolean authorizationRequired
) {

    public CreateProcessRequest {
        selectedFiles = selectedFiles == null ? List.of() : List.copyOf(selectedFiles);
    }
}
