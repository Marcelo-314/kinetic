package com.chronicle.adapters.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateProcessRequestDto(
        @NotBlank String objective,
        @NotBlank String sourceFolder,
        @NotNull String selectionMode,
        List<String> selectedFiles,
        @Min(1) int batchSize,
        @NotNull String summaryPolicy,
        @NotNull String failurePolicy,
        @NotNull Boolean authorizationRequired
) {
}
