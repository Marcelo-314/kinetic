package com.chronicle.adapters.in.web.dto;

public record TotalsViewDto(
        long totalWords,
        long totalLines,
        long totalCharacters
) {
}
