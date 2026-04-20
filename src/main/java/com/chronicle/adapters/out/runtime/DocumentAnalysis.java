package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.WordFrequency;

import java.util.List;

record DocumentAnalysis(
        int wordCount,
        int lineCount,
        int characterCount,
        List<WordFrequency> mostFrequentWords,
        String summary
) {
}
