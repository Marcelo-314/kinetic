package com.chronicle.application.usecase;

import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.domain.command.AuthorizeProcess;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.transition.TransitionContext;
import com.chronicle.domain.transition.TransitionEngine;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class AuthorizeProcessUseCase {

    private final ProcessRepository processRepository;
    private final ProcessPlanRepository processPlanRepository;
    private final AuthorizationInfoRepository authorizationInfoRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final DocumentExecutionRepository documentExecutionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final TransitionEngine transitionEngine;

    public AuthorizeProcessUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            DocumentExecutionRepository documentExecutionRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        this.processRepository = processRepository;
        this.processPlanRepository = processPlanRepository;
        this.authorizationInfoRepository = authorizationInfoRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.documentExecutionRepository = documentExecutionRepository;
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.transitionEngine = transitionEngine;
    }

    public ProcessAggregate execute(ProcessCommandRequest request) {
        var now = clockPort.now();
        ProcessAggregate process = processRepository.findById(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));

        ProcessAggregate newProcess = transitionEngine.apply(process, new AuthorizeProcess(), TransitionContext.empty())
                .newAggregate()
                .touch(now);
        processRepository.save(newProcess);

        var plan = processPlanRepository.findByProcessId(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));
        for (int index = 0; index < plan.selectedFiles().size(); index += 1) {
            String documentName = plan.selectedFiles().get(index);
            documentExecutionRepository.save(new DocumentExecution(
                    idGeneratorPort.generate(),
                    request.processId(),
                    documentName,
                    Path.of(plan.sourceFolder(), documentName).toString(),
                    DocumentStatus.PENDING,
                    (index / Math.max(plan.batchSize(), 1)) + 1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null
            ));
        }

        AuthorizationInfo authorizationInfo = authorizationInfoRepository.findByProcessId(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));
        authorizationInfoRepository.save(new AuthorizationInfo(
                authorizationInfo.authorizationInfoId(),
                authorizationInfo.processId(),
                authorizationInfo.authorizationRequired(),
                AuthorizationState.AUTHORIZED,
                null,
                now,
                authorizationInfo.authorizationNote(),
                now
        ));

        ProgressSnapshot progressSnapshot = progressSnapshotRepository.findByProcessId(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));
        progressSnapshotRepository.save(new ProgressSnapshot(
                progressSnapshot.progressSnapshotId(),
                progressSnapshot.processId(),
                progressSnapshot.totalFiles(),
                progressSnapshot.processedFiles(),
                progressSnapshot.successfulFiles(),
                progressSnapshot.failedFiles(),
                progressSnapshot.pendingFiles(),
                progressSnapshot.percentage(),
                progressSnapshot.currentBatchIndex(),
                progressSnapshot.currentBatchSize(),
                now,
                progressSnapshot.estimatedCompletion(),
                progressSnapshot.lastProgressAt()
        ));

        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                request.processId(),
                now,
                "PROCESS_AUTHORIZED",
                "APPLICATION",
                "Process authorized and moved to RUNNING.",
                Map.of("status", newProcess.state().code()),
                request.processId()
        ));

        return newProcess;
    }
}
