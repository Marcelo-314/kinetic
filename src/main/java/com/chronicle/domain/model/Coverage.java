package com.chronicle.domain.model;

public record Coverage(
        int plannedFiles,
        int includedFiles,
        int excludedFiles,
        double percentage
) {

    public Coverage {
        if (plannedFiles < 0 || includedFiles < 0 || excludedFiles < 0) {
            throw new IllegalArgumentException("coverage counts must not be negative");
        }
        if (percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException("coverage percentage must be between zero and one hundred");
        }
    }
}
