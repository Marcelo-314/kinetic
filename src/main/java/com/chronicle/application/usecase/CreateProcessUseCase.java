package com.chronicle.application.usecase;

import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.application.exception.SemanticValidationException;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.FileSourcePort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CreateProcessUseCase {

    private static final String AWAITING_AUTHORIZATION = "AWAITING_AUTHORIZATION";

    private final ProcessRepository processRepository;
    private final ProcessPlanRepository processPlanRepository;
    private final AuthorizationInfoRepository authorizationInfoRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final ExecutionControlFlagsRepository executionControlFlagsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final FileSourcePort fileSourcePort;

    public CreateProcessUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            FileSourcePort fileSourcePort
    ) {
        this.processRepository = processRepository;
        this.processPlanRepository = processPlanRepository;
        this.authorizationInfoRepository = authorizationInfoRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.executionControlFlagsRepository = executionControlFlagsRepository;
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.fileSourcePort = fileSourcePort;
    }

    public ProcessAggregate execute(CreateProcessRequest request) {
        validateRequest(request);

        Instant now = clockPort.now();
        String processId = idGeneratorPort.generate();
        List<String> selectedFiles = resolveSelectedFiles(request);
        ProcessAggregate process = ProcessAggregate.pending(processId, request.objective(), now);

        ProcessPlan plan = new ProcessPlan(
                idGeneratorPort.generate(),
                processId,
                request.sourceFolder(),
                request.selectionMode(),
                selectedFiles,
                selectedFiles.size(),
                request.batchSize(),
                request.summaryPolicy(),
                request.failurePolicy(),
                now
        );

        AuthorizationInfo authorizationInfo = new AuthorizationInfo(
                idGeneratorPort.generate(),
                processId,
                request.authorizationRequired(),
                request.authorizationRequired() ? AuthorizationState.WAITING : AuthorizationState.NOT_REQUIRED,
                request.authorizationRequired() ? AWAITING_AUTHORIZATION : null,
                null,
                null,
                now
        );

        ProgressSnapshot progressSnapshot = new ProgressSnapshot(
                idGeneratorPort.generate(),
                processId,
                selectedFiles.size(),
                0,
                0,
                0,
                selectedFiles.size(),
                0.0,
                null,
                null,
                null,
                null,
                null
        );

        ExecutionControlFlags executionControlFlags = new ExecutionControlFlags(
                processId,
                false,
                false,
                null,
                null
        );

        ActivityLogEntry activity = new ActivityLogEntry(
                idGeneratorPort.generate(),
                processId,
                now,
                "PROCESS_CREATED",
                "APPLICATION",
                "Process created in PENDING state.",
                Map.of("status", process.state().code()),
                processId
        );

        processRepository.save(process);
        processPlanRepository.save(plan);
        authorizationInfoRepository.save(authorizationInfo);
        progressSnapshotRepository.save(progressSnapshot);
        executionControlFlagsRepository.save(executionControlFlags);
        activityLogRepository.save(activity);

        return process;
    }

    private void validateRequest(CreateProcessRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.sourceFolder() == null || request.sourceFolder().isBlank()) {
            throw new IllegalArgumentException("sourceFolder must not be blank");
        }
        if (!fileSourcePort.folderExists(request.sourceFolder())) {
            throw new SemanticValidationException(
                    "sourceFolder must exist",
                    Map.of("source_folder", request.sourceFolder())
            );
        }
        if (request.batchSize() <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (request.selectionMode() == SelectionMode.EXPLICIT_SELECTION && request.selectedFiles().isEmpty()) {
            throw new SemanticValidationException("selectedFiles must not be empty for explicit selection");
        }
    }

    private List<String> resolveSelectedFiles(CreateProcessRequest request) {
        if (request.selectionMode() == SelectionMode.ALL_FROM_FOLDER) {
            return List.copyOf(fileSourcePort.listTextFiles(request.sourceFolder()));
        }
        return request.selectedFiles();
    }
}
