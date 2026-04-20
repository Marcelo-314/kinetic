package com.chronicle.adapters.in.web.dto;

import java.util.List;

public record DocumentResultDto(
        String documentName,
        String documentStatus,
        Integer wordCount,
        Integer lineCount,
        Integer characterCount,
        List<WordFrequencyDto> mostFrequentWords,
        String summary,
        String summaryMethod,
        String errorCode,
        String errorMessage
) {
}
