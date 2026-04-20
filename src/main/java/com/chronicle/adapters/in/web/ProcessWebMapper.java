package com.chronicle.adapters.in.web;

import com.chronicle.adapters.in.web.dto.AuthorizationViewDto;
import com.chronicle.adapters.in.web.dto.CommandAcceptedResponseDto;
import com.chronicle.adapters.in.web.dto.CoverageViewDto;
import com.chronicle.adapters.in.web.dto.CreateProcessRequestDto;
import com.chronicle.adapters.in.web.dto.CreateProcessResponseDto;
import com.chronicle.adapters.in.web.dto.DocumentResultDto;
import com.chronicle.adapters.in.web.dto.ExcludedDocumentDto;
import com.chronicle.adapters.in.web.dto.ProcessListItemDto;
import com.chronicle.adapters.in.web.dto.ProcessListProgressDto;
import com.chronicle.adapters.in.web.dto.ProcessListResponseDto;
import com.chronicle.adapters.in.web.dto.ProcessPlanDto;
import com.chronicle.adapters.in.web.dto.ProcessResultsResponseDto;
import com.chronicle.adapters.in.web.dto.ProcessStatusResponseDto;
import com.chronicle.adapters.in.web.dto.ProgressViewDto;
import com.chronicle.adapters.in.web.dto.TerminalInfoDto;
import com.chronicle.adapters.in.web.dto.TotalsViewDto;
import com.chronicle.adapters.in.web.dto.WordFrequencyDto;
import com.chronicle.application.query.ProcessListItemView;
import com.chronicle.application.query.ProcessListView;
import com.chronicle.application.query.ProcessResultsView;
import com.chronicle.application.query.ProcessStatusView;
import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProcessWebMapper {

    public CreateProcessRequest toApplicationRequest(CreateProcessRequestDto dto) {
        return new CreateProcessRequest(
                dto.objective(),
                dto.sourceFolder(),
                SelectionMode.valueOf(dto.selectionMode()),
                dto.selectedFiles() == null ? List.of() : dto.selectedFiles(),
                dto.batchSize(),
                SummaryPolicy.valueOf(dto.summaryPolicy()),
                FailurePolicy.valueOf(dto.failurePolicy()),
                dto.authorizationRequired()
        );
    }

    public CreateProcessResponseDto toCreateResponse(ProcessStatusView view) {
        return new CreateProcessResponseDto(
                view.processId(),
                view.status(),
                "Process created and awaiting authorization.",
                toAuthorizationView(view),
                linksFor(view.processId())
        );
    }

    public CommandAcceptedResponseDto toAcceptedResponse(ProcessAggregate aggregate, String message) {
        return new CommandAcceptedResponseDto(
                aggregate.processId(),
                aggregate.state().code(),
                message
        );
    }

    public ProcessStatusResponseDto toStatusResponse(ProcessStatusView view) {
        return new ProcessStatusResponseDto(
                view.processId(),
                view.status(),
                view.version(),
                view.createdAt(),
                view.updatedAt(),
                view.objective(),
                new ProcessPlanDto(
                        view.plan().sourceFolder(),
                        view.plan().selectionMode().name(),
                        view.plan().selectedFiles(),
                        view.plan().totalPlannedFiles(),
                        view.plan().batchSize(),
                        view.plan().summaryPolicy().name(),
                        view.plan().failurePolicy().name()
                ),
                toAuthorizationView(view),
                new ProgressViewDto(
                        view.progress().totalFiles(),
                        view.progress().processedFiles(),
                        view.progress().successfulFiles(),
                        view.progress().failedFiles(),
                        view.progress().pendingFiles(),
                        view.progress().percentage(),
                        view.progress().startedAt(),
                        view.progress().estimatedCompletion(),
                        view.progress().lastProgressAt()
                ),
                view.resultKind().name(),
                view.terminalInfo() == null ? null : new TerminalInfoDto(
                        view.terminalInfo().terminalState(),
                        view.terminalInfo().terminalAt(),
                        view.terminalInfo().terminalReasonCode(),
                        view.terminalInfo().terminalReasonMessage()
                ),
                linksFor(view.processId())
        );
    }

    public ProcessListResponseDto toListResponse(ProcessListView view) {
        return new ProcessListResponseDto(
                view.items().stream().map(this::toListItem).toList(),
                view.page(),
                view.pageSize(),
                view.totalItems()
        );
    }

    public ProcessResultsResponseDto toResultsResponse(ProcessResultsView view) {
        return new ProcessResultsResponseDto(
                view.processId(),
                view.processStatus(),
                view.resultKind(),
                view.computedAt(),
                new CoverageViewDto(
                        view.plannedFiles(),
                        view.includedFiles(),
                        view.excludedFiles(),
                        view.coveragePercentage()
                ),
                new TotalsViewDto(
                        view.totalWords(),
                        view.totalLines(),
                        view.totalCharacters()
                ),
                view.mostFrequentWords().stream().map(word -> new WordFrequencyDto(word.term(), word.count())).toList(),
                view.globalSummary(),
                view.documents().stream().map(this::toDocumentResult).toList(),
                view.excludedDocuments().stream()
                        .map(document -> new ExcludedDocumentDto(document.documentName(), document.reasonCode()))
                        .toList(),
                view.terminalInfo() == null ? null : new TerminalInfoDto(
                        view.terminalInfo().terminalState(),
                        view.terminalInfo().terminalAt(),
                        view.terminalInfo().terminalReasonCode(),
                        view.terminalInfo().terminalReasonMessage()
                )
        );
    }

    private ProcessListItemDto toListItem(ProcessListItemView item) {
        return new ProcessListItemDto(
                item.processId(),
                item.status(),
                item.createdAt(),
                item.updatedAt(),
                item.resultKind().name(),
                new ProcessListProgressDto(item.totalFiles(), item.processedFiles(), item.percentage())
        );
    }

    private AuthorizationViewDto toAuthorizationView(ProcessStatusView view) {
        return new AuthorizationViewDto(
                view.authorization().authorizationRequired(),
                view.authorization().authorizationState().name(),
                view.authorization().pendingReason(),
                view.authorization().authorizedAt()
        );
    }

    private DocumentResultDto toDocumentResult(DocumentExecution document) {
        return new DocumentResultDto(
                document.documentName(),
                document.documentStatus().name(),
                document.wordCount(),
                document.lineCount(),
                document.characterCount(),
                document.mostFrequentWords().stream().map(word -> new WordFrequencyDto(word.term(), word.count())).toList(),
                document.summary(),
                document.summaryMethod() == null ? null : document.summaryMethod().name(),
                document.errorCode(),
                document.errorMessage()
        );
    }

    private Map<String, String> linksFor(String processId) {
        return Map.of(
                "status", "/api/v1/processes/" + processId + "/status",
                "authorize", "/api/v1/processes/" + processId + "/authorize",
                "pause", "/api/v1/processes/" + processId + "/pause",
                "resume", "/api/v1/processes/" + processId + "/resume",
                "stop", "/api/v1/processes/" + processId + "/stop",
                "results", "/api/v1/processes/" + processId + "/results",
                "activity", "/api/v1/processes/" + processId + "/activity"
        );
    }
}
