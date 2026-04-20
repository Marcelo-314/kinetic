package com.chronicle.adapters.in.web.dto;

public record ProcessListProgressDto(
        int totalFiles,
        int processedFiles,
        double percentage
) {
}
