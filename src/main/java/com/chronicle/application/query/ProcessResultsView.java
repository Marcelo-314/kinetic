package com.chronicle.application.query;

import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.ExcludedDocument;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.model.WordFrequency;

import java.time.Instant;
import java.util.List;

public record ProcessResultsView(
        String processId,
        String processStatus,
        String resultKind,
        Instant computedAt,
        int plannedFiles,
        int includedFiles,
        int excludedFiles,
        double coveragePercentage,
        long totalWords,
        long totalLines,
        long totalCharacters,
        List<WordFrequency> mostFrequentWords,
        String globalSummary,
        List<DocumentExecution> documents,
        List<ExcludedDocument> excludedDocuments,
        TerminalInfo terminalInfo
) {
}
