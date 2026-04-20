package com.chronicle.application.service;

import com.chronicle.domain.model.Coverage;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.ExcludedDocument;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessResult;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.WordFrequency;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessResultRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ProcessResultProjectionService {

    private final ProcessPlanRepository processPlanRepository;
    private final DocumentExecutionRepository documentExecutionRepository;
    private final ProcessResultRepository processResultRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;

    public ProcessResultProjectionService(
            ProcessPlanRepository processPlanRepository,
            DocumentExecutionRepository documentExecutionRepository,
            ProcessResultRepository processResultRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort
    ) {
        this.processPlanRepository = processPlanRepository;
        this.documentExecutionRepository = documentExecutionRepository;
        this.processResultRepository = processResultRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    public Optional<ProcessResult> findCurrent(String processId) {
        return processResultRepository.findCurrentByProcessId(processId);
    }

    public Optional<ProcessResult> snapshotCurrent(ProcessAggregate process) {
        ProcessResult projection = buildProjection(process, clockPort.now());
        if (!shouldPersistSnapshot(projection, process)) {
            return Optional.empty();
        }
        return Optional.of(processResultRepository.save(projection));
    }

    public ProcessResult computeCurrent(ProcessAggregate process) {
        return processResultRepository.findCurrentByProcessId(process.processId())
                .orElseGet(() -> buildProjection(process, clockPort.now()));
    }

    private boolean shouldPersistSnapshot(ProcessResult projection, ProcessAggregate process) {
        if (projection.resultKind() != ResultKind.NONE) {
            return true;
        }
        return "COMPLETED".equals(process.state().code());
    }

    private ProcessResult buildProjection(ProcessAggregate process, Instant computedAt) {
        var plan = processPlanRepository.findByProcessId(process.processId())
                .orElseThrow(() -> new IllegalStateException("Missing process plan for process " + process.processId()));

        List<DocumentExecution> closedDocuments = documentExecutionRepository.findByProcessId(process.processId()).stream()
                .filter(this::isClosedDocument)
                .toList();
        List<DocumentExecution> processedDocuments = closedDocuments.stream()
                .filter(document -> document.documentStatus() == DocumentStatus.PROCESSED)
                .toList();
        List<ExcludedDocument> excludedDocuments = closedDocuments.stream()
                .filter(document -> document.documentStatus() != DocumentStatus.PROCESSED)
                .map(this::toExcludedDocument)
                .toList();

        long totalWords = processedDocuments.stream()
                .map(DocumentExecution::wordCount)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();
        long totalLines = processedDocuments.stream()
                .map(DocumentExecution::lineCount)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();
        long totalCharacters = processedDocuments.stream()
                .map(DocumentExecution::characterCount)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();

        int includedFiles = processedDocuments.size();
        int excludedFiles = excludedDocuments.size();
        int plannedFiles = plan.totalPlannedFiles();
        double coveragePercentage = plannedFiles == 0
                ? 0.0
                : (includedFiles * 100.0) / plannedFiles;

        ResultKind resultKind = determineResultKind(process, closedDocuments.isEmpty());
        String globalSummary = resultKind == ResultKind.FINAL
                ? processedDocuments.stream()
                        .map(DocumentExecution::summary)
                        .filter(summary -> summary != null && !summary.isBlank())
                        .reduce((left, right) -> left + System.lineSeparator() + System.lineSeparator() + right)
                        .orElse(null)
                : null;

        return new ProcessResult(
                idGeneratorPort.generate(),
                process.processId(),
                resultKind,
                computedAt,
                processedDocuments.stream().map(DocumentExecution::documentExecutionId).toList(),
                excludedDocuments,
                totalWords,
                totalLines,
                totalCharacters,
                aggregateTopWords(processedDocuments),
                processedDocuments.stream()
                        .filter(document -> document.summary() != null && !document.summary().isBlank())
                        .collect(LinkedHashMap::new, (summaries, document) -> summaries.put(document.documentName(), document.summary()), Map::putAll),
                globalSummary,
                new Coverage(plannedFiles, includedFiles, excludedFiles, coveragePercentage),
                resultKind == ResultKind.FINAL
        );
    }

    private ResultKind determineResultKind(ProcessAggregate process, boolean hasNoClosedEvidence) {
        if ("COMPLETED".equals(process.state().code())) {
            return ResultKind.FINAL;
        }
        if (hasNoClosedEvidence) {
            return ResultKind.NONE;
        }
        // STOPPED and FAILED keep NONE until there is closed documentary evidence; otherwise they expose PARTIAL.
        return ResultKind.PARTIAL;
    }

    private boolean isClosedDocument(DocumentExecution documentExecution) {
        return documentExecution.documentStatus() == DocumentStatus.PROCESSED
                || documentExecution.documentStatus() == DocumentStatus.FAILED
                || documentExecution.documentStatus() == DocumentStatus.SKIPPED;
    }

    private ExcludedDocument toExcludedDocument(DocumentExecution documentExecution) {
        String reasonCode = switch (documentExecution.documentStatus()) {
            case FAILED -> "DOCUMENT_FAILED";
            case SKIPPED -> "DOCUMENT_SKIPPED";
            default -> "DOCUMENT_EXCLUDED";
        };
        return new ExcludedDocument(documentExecution.documentName(), reasonCode);
    }

    private List<WordFrequency> aggregateTopWords(List<DocumentExecution> processedDocuments) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DocumentExecution document : processedDocuments) {
            for (WordFrequency word : document.mostFrequentWords()) {
                counts.merge(word.term(), word.count(), Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                .toList();
    }
}
