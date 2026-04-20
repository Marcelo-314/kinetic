package com.chronicle.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProcessResult(
        String processResultId,
        String processId,
        ResultKind resultKind,
        Instant computedAt,
        List<String> includedDocuments,
        List<ExcludedDocument> excludedDocuments,
        long totalWords,
        long totalLines,
        long totalCharacters,
        List<WordFrequency> mostFrequentWords,
        Map<String, String> documentSummaries,
        String globalSummary,
        Coverage coverage,
        boolean isFinal
) {

    public ProcessResult {
        if (processResultId == null || processResultId.isBlank()) {
            throw new IllegalArgumentException("processResultId must not be blank");
        }
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("processId must not be blank");
        }
        if (resultKind == null) {
            throw new IllegalArgumentException("resultKind must not be null");
        }
        if (computedAt == null) {
            throw new IllegalArgumentException("computedAt must not be null");
        }
        if (coverage == null) {
            throw new IllegalArgumentException("coverage must not be null");
        }
        if (totalWords < 0 || totalLines < 0 || totalCharacters < 0) {
            throw new IllegalArgumentException("totals must not be negative");
        }

        includedDocuments = includedDocuments == null ? List.of() : List.copyOf(includedDocuments);
        excludedDocuments = excludedDocuments == null ? List.of() : List.copyOf(excludedDocuments);
        mostFrequentWords = mostFrequentWords == null ? List.of() : List.copyOf(mostFrequentWords);
        documentSummaries = documentSummaries == null ? Map.of() : Map.copyOf(documentSummaries);
    }
}
