package com.chronicle.application.usecase;

import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.application.query.ProcessStatusView;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.TerminalInfoRepository;

public final class GetProcessStatusUseCase {

    private final ProcessRepository processRepository;
    private final ProcessPlanRepository processPlanRepository;
    private final AuthorizationInfoRepository authorizationInfoRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final TerminalInfoRepository terminalInfoRepository;

    public GetProcessStatusUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            TerminalInfoRepository terminalInfoRepository
    ) {
        this.processRepository = processRepository;
        this.processPlanRepository = processPlanRepository;
        this.authorizationInfoRepository = authorizationInfoRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.terminalInfoRepository = terminalInfoRepository;
    }

    public ProcessStatusView execute(String processId) {
        var process = processRepository.findById(processId)
                .orElseThrow(() -> new ProcessNotFoundException(processId));

        var plan = processPlanRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Process plan not found for process " + processId));
        var authorization = authorizationInfoRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Authorization info not found for process " + processId));
        var progress = progressSnapshotRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Progress snapshot not found for process " + processId));
        var terminalInfo = terminalInfoRepository.findByProcessId(processId).orElse(null);

        return new ProcessStatusView(
                process.processId(),
                process.state().code(),
                process.version(),
                process.createdAt(),
                process.updatedAt(),
                process.objective(),
                plan,
                authorization,
                progress,
                process.resultKind(),
                terminalInfo
        );
    }
}
