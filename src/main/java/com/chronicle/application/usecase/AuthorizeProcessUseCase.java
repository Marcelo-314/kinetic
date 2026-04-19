package com.chronicle.application.usecase;

import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.domain.command.AuthorizeProcess;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.transition.TransitionContext;
import com.chronicle.domain.transition.TransitionEngine;

import java.util.Map;

public final class AuthorizeProcessUseCase {

    private final ProcessRepository processRepository;
    private final AuthorizationInfoRepository authorizationInfoRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final TransitionEngine transitionEngine;

    public AuthorizeProcessUseCase(
            ProcessRepository processRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        this.processRepository = processRepository;
        this.authorizationInfoRepository = authorizationInfoRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.transitionEngine = transitionEngine;
    }

    public ProcessAggregate execute(ProcessCommandRequest request) {
        ProcessAggregate process = processRepository.findById(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));

        ProcessAggregate newProcess = transitionEngine.apply(process, new AuthorizeProcess(), TransitionContext.empty()).newAggregate();
        processRepository.save(newProcess);

        AuthorizationInfo authorizationInfo = authorizationInfoRepository.findByProcessId(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));
        authorizationInfoRepository.save(new AuthorizationInfo(
                authorizationInfo.authorizationInfoId(),
                authorizationInfo.processId(),
                authorizationInfo.authorizationRequired(),
                AuthorizationState.AUTHORIZED,
                null,
                clockPort.now(),
                authorizationInfo.authorizationNote(),
                clockPort.now()
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
                clockPort.now(),
                progressSnapshot.estimatedCompletion(),
                progressSnapshot.lastProgressAt()
        ));

        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                request.processId(),
                clockPort.now(),
                "PROCESS_AUTHORIZED",
                "APPLICATION",
                "Process authorized and moved to RUNNING.",
                Map.of("status", newProcess.state().code()),
                request.processId()
        ));

        return newProcess;
    }
}
