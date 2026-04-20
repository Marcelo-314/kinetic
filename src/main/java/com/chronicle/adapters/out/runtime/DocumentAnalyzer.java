package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.WordFrequency;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Component
public class DocumentAnalyzer {

    public DocumentAnalysis analyze(String content) {
        String normalizedContent = content == null ? "" : content;
        int characterCount = normalizedContent.length();
        int lineCount = (int) normalizedContent.lines().count();

        Map<String, Long> wordCounts = Arrays.stream(normalizedContent.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(Function.identity(), java.util.stream.Collectors.counting()));

        var topWords = wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .map(entry -> new WordFrequency(entry.getKey(), Math.toIntExact(entry.getValue())))
                .toList();

        int wordCount = wordCounts.values().stream().mapToInt(Long::intValue).sum();
        String summary = summarize(normalizedContent);

        return new DocumentAnalysis(wordCount, lineCount, characterCount, topWords, summary);
    }

    private String summarize(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .map(line -> line.length() <= 160 ? line : line.substring(0, 160))
                .orElse("");
    }
}
