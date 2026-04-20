package com.chronicle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentExecutionTest {

    @Test
    void mostFrequentWordsIsDefensivelyCopied() {
        List<WordFrequency> words = new ArrayList<>();
        words.add(new WordFrequency("chronicle", 3));

        DocumentExecution execution = new DocumentExecution(
                "doc-exec-1",
                "process-1",
                "file.txt",
                "/tmp/file.txt",
                DocumentStatus.PROCESSED,
                0,
                Instant.parse("2026-04-19T18:00:00Z"),
                Instant.parse("2026-04-19T18:01:00Z"),
                120,
                12,
                640,
                words,
                "Summary",
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                null,
                null
        );

        words.add(new WordFrequency("later", 1));

        assertEquals(1, execution.mostFrequentWords().size());
        assertThrows(UnsupportedOperationException.class, () -> execution.mostFrequentWords().add(new WordFrequency("x", 1)));
    }
}
