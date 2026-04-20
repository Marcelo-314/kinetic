package com.chronicle.adapters.in.web.dto;

import java.time.Instant;
import java.util.List;

public record ProcessResultsResponseDto(
        String processId,
        String processStatus,
        String resultKind,
        Instant computedAt,
        CoverageViewDto coverage,
        TotalsViewDto totals,
        List<WordFrequencyDto> mostFrequentWords,
        String globalSummary,
        List<DocumentResultDto> documents,
        List<ExcludedDocumentDto> excludedDocuments,
        TerminalInfoDto terminalInfo
) {
}
