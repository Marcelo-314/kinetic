package com.chronicle.domain.model;

import java.time.Instant;
import java.util.List;

public record DocumentExecution(
        String documentExecutionId,
        String processId,
        String documentName,
        String documentPath,
        DocumentStatus documentStatus,
        int batchIndex,
        Instant startedAt,
        Instant finishedAt,
        Integer wordCount,
        Integer lineCount,
        Integer characterCount,
        List<WordFrequency> mostFrequentWords,
        String summary,
        SummaryPolicy summaryMethod,
        String errorCode,
        String errorMessage
) {

    public DocumentExecution {
        if (documentExecutionId == null || documentExecutionId.isBlank()) {
            throw new IllegalArgumentException("documentExecutionId must not be blank");
        }
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("processId must not be blank");
        }
        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("documentName must not be blank");
        }
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("documentPath must not be blank");
        }
        if (documentStatus == null) {
            throw new IllegalArgumentException("documentStatus must not be null");
        }
        if (batchIndex < 0) {
            throw new IllegalArgumentException("batchIndex must not be negative");
        }

        mostFrequentWords = mostFrequentWords == null ? List.of() : List.copyOf(mostFrequentWords);
    }
}
