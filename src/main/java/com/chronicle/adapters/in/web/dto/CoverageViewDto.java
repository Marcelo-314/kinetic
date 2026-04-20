package com.chronicle.adapters.in.web.dto;

public record CoverageViewDto(
        int plannedFiles,
        int includedFiles,
        int excludedFiles,
        double percentage
) {
}
