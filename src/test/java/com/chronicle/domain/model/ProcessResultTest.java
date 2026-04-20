package com.chronicle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessResultTest {

    @Test
    void collectionsAreDefensivelyCopied() {
        List<String> includedDocuments = new ArrayList<>(List.of("a.txt"));
        var summaries = new HashMap<String, String>();
        summaries.put("a.txt", "Summary");

        ProcessResult result = new ProcessResult(
                "result-1",
                "process-1",
                ResultKind.PARTIAL,
                Instant.parse("2026-04-19T18:05:00Z"),
                includedDocuments,
                List.of(new ExcludedDocument("b.txt", "FAILED")),
                10,
                2,
                50,
                List.of(new WordFrequency("chronicle", 4)),
                summaries,
                "Global",
                new Coverage(2, 1, 1, 50.0),
                false
        );

        includedDocuments.add("later.txt");
        summaries.put("later.txt", "Later");

        assertEquals(1, result.includedDocuments().size());
        assertEquals(1, result.documentSummaries().size());
        assertThrows(UnsupportedOperationException.class, () -> result.includedDocuments().add("x"));
    }
}
