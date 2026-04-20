package com.chronicle.application.usecase;

import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.application.query.GetProcessResultsQuery;
import com.chronicle.application.query.ProcessResultsView;
import com.chronicle.application.service.ProcessResultProjectionService;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.model.WordFrequency;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.TerminalInfoRepository;

import java.util.List;

public final class GetProcessResultsUseCase {

    private final ProcessRepository processRepository;
    private final DocumentExecutionRepository documentExecutionRepository;
    private final TerminalInfoRepository terminalInfoRepository;
    private final ProcessResultProjectionService processResultProjectionService;

    public GetProcessResultsUseCase(
            ProcessRepository processRepository,
            DocumentExecutionRepository documentExecutionRepository,
            TerminalInfoRepository terminalInfoRepository,
            ProcessResultProjectionService processResultProjectionService
    ) {
        this.processRepository = processRepository;
        this.documentExecutionRepository = documentExecutionRepository;
        this.terminalInfoRepository = terminalInfoRepository;
        this.processResultProjectionService = processResultProjectionService;
    }

    public ProcessResultsView execute(GetProcessResultsQuery query) {
        var process = processRepository.findById(query.processId())
                .orElseThrow(() -> new ProcessNotFoundException(query.processId()));
        var result = processResultProjectionService.computeCurrent(process);
        TerminalInfo terminalInfo = terminalInfoRepository.findByProcessId(query.processId()).orElse(null);

        List<DocumentExecution> closedDocuments = query.includeDocuments()
                ? documentExecutionRepository.findByProcessId(query.processId()).stream()
                        .filter(document -> document.finishedAt() != null)
                        .map(document -> trimDocumentTopWords(document, query.topWordsLimit()))
                        .toList()
                : List.of();

        return new ProcessResultsView(
                process.processId(),
                process.state().code(),
                result.resultKind().name(),
                result.computedAt(),
                result.coverage().plannedFiles(),
                result.coverage().includedFiles(),
                result.coverage().excludedFiles(),
                result.coverage().percentage(),
                result.totalWords(),
                result.totalLines(),
                result.totalCharacters(),
                trimTopWords(result.mostFrequentWords(), query.topWordsLimit()),
                query.includeGlobalSummary() ? result.globalSummary() : null,
                closedDocuments,
                result.excludedDocuments(),
                terminalInfo
        );
    }

    private List<WordFrequency> trimTopWords(List<WordFrequency> words, int limit) {
        return words.stream().limit(limit).toList();
    }

    private DocumentExecution trimDocumentTopWords(DocumentExecution documentExecution, int limit) {
        return new DocumentExecution(
                documentExecution.documentExecutionId(),
                documentExecution.processId(),
                documentExecution.documentName(),
                documentExecution.documentPath(),
                documentExecution.documentStatus(),
                documentExecution.batchIndex(),
                documentExecution.startedAt(),
                documentExecution.finishedAt(),
                documentExecution.wordCount(),
                documentExecution.lineCount(),
                documentExecution.characterCount(),
                trimTopWords(documentExecution.mostFrequentWords(), limit),
                documentExecution.summary(),
                documentExecution.summaryMethod(),
                documentExecution.errorCode(),
                documentExecution.errorMessage()
        );
    }
}
