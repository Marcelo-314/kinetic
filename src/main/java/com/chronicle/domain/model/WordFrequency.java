package com.chronicle.domain.model;

public record WordFrequency(
        String term,
        int count
) {

    public WordFrequency {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("term must not be blank");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
    }
}
