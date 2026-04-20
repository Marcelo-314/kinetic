package com.chronicle.application.query;

public record GetProcessResultsQuery(
        String processId,
        boolean includeDocuments,
        boolean includeGlobalSummary,
        int topWordsLimit
) {
}
