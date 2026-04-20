package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.command.ResolveCheckpoint;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.FileSourcePort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.TerminalInfoRepository;
import com.chronicle.domain.transition.TransitionContext;
import com.chronicle.domain.transition.TransitionEngine;
import com.chronicle.application.service.ProcessResultProjectionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProcessWorker {

    private final ProcessRepository processRepository;
    private final ProcessPlanRepository processPlanRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final ExecutionControlFlagsRepository executionControlFlagsRepository;
    private final DocumentExecutionRepository documentExecutionRepository;
    private final TerminalInfoRepository terminalInfoRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FileSourcePort fileSourcePort;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final ProcessLeasePort processLeasePort;
    private final TransitionEngine transitionEngine;
    private final DocumentAnalyzer documentAnalyzer;
    private final ProcessResultProjectionService processResultProjectionService;
    private final RuntimeTelemetry runtimeTelemetry;
    private final TransactionTemplate transactionTemplate;

    public ProcessWorker(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            DocumentExecutionRepository documentExecutionRepository,
            TerminalInfoRepository terminalInfoRepository,
            ActivityLogRepository activityLogRepository,
            FileSourcePort fileSourcePort,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            ProcessLeasePort processLeasePort,
            TransitionEngine transitionEngine,
            DocumentAnalyzer documentAnalyzer,
            ProcessResultProjectionService processResultProjectionService,
            RuntimeTelemetry runtimeTelemetry,
            TransactionTemplate transactionTemplate
    ) {
        this.processRepository = processRepository;
        this.processPlanRepository = processPlanRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.executionControlFlagsRepository = executionControlFlagsRepository;
        this.documentExecutionRepository = documentExecutionRepository;
        this.terminalInfoRepository = terminalInfoRepository;
        this.activityLogRepository = activityLogRepository;
        this.fileSourcePort = fileSourcePort;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.processLeasePort = processLeasePort;
        this.transitionEngine = transitionEngine;
        this.documentAnalyzer = documentAnalyzer;
        this.processResultProjectionService = processResultProjectionService;
        this.runtimeTelemetry = runtimeTelemetry;
        this.transactionTemplate = transactionTemplate;
    }

    public void processNextDocument(String processId) {
        processNextDocument(processId, null, null);
    }

    public void processNextDocument(String processId, String ownerId, Duration leaseDuration) {
        PreparedProcessStep preparedStep = transactionTemplate.execute(status -> prepareStep(processId));
        if (preparedStep == null || !preparedStep.hasDocumentWork()) {
            return;
        }

        runtimeTelemetry.recordWorkerStarted(preparedStep.process().processId(), ownerId, preparedStep.process().state().code());
        DocumentAnalysis analysis = null;
        RuntimeException processingFailure = null;
        var documentTimer = runtimeTelemetry.startDocumentProcessingSample();
        LeaseRenewalSession renewalSession = startLeaseRenewal(processId, ownerId, leaseDuration);
        try (LeaseRenewalSession ignored = renewalSession) {
            try {
                analysis = documentAnalyzer.analyze(
                        fileSourcePort.readTextFile(preparedStep.plan().sourceFolder(), preparedStep.documentExecution().documentName())
                );
            } catch (RuntimeException exception) {
                processingFailure = exception;
            }
        }

        DocumentAnalysis finalAnalysis = analysis;
        RuntimeException finalProcessingFailure = processingFailure;
        transactionTemplate.executeWithoutResult(status ->
                closeStep(preparedStep, finalAnalysis, finalProcessingFailure, renewalSession.leaseLost().get())
        );
        runtimeTelemetry.recordDocumentProcessingDuration(documentTimer, renewalSession.leaseLost().get()
                ? "lease_lost"
                : (finalProcessingFailure == null ? "success" : "failure"));
    }

    private PreparedProcessStep prepareStep(String processId) {
        ProcessAggregate process = processRepository.findById(processId).orElse(null);
        if (process == null || !"RUNNING".equals(process.state().code())) {
            return null;
        }

        var plan = processPlanRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Missing plan for process " + processId));
        var progress = progressSnapshotRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Missing progress for process " + processId));
        var flags = executionControlFlagsRepository.findByProcessId(processId)
                .orElseThrow(() -> new IllegalStateException("Missing execution flags for process " + processId));

        if (documentExecutionRepository.findProcessingByProcessId(processId).isPresent()) {
            throw new IllegalStateException("A process cannot have more than one document in PROCESSING");
        }

        if (process.stopRequested() || process.pauseRequested()) {
            resolveCheckpoint(process, progress, flags, false);
            return null;
        }

        var nextPending = documentExecutionRepository.findNextPendingByProcessId(processId);
        if (nextPending.isEmpty()) {
            resolveCheckpoint(process, progress, flags, true);
            return null;
        }

        Instant now = clockPort.now();
        DocumentExecution processingDocument = toProcessing(nextPending.orElseThrow(), now);
        documentExecutionRepository.save(processingDocument);
        runtimeTelemetry.recordDocumentProcessingStarted(process, processingDocument, null);
        progressSnapshotRepository.save(new ProgressSnapshot(
                progress.progressSnapshotId(),
                progress.processId(),
                progress.totalFiles(),
                progress.processedFiles(),
                progress.successfulFiles(),
                progress.failedFiles(),
                progress.pendingFiles(),
                progress.percentage(),
                processingDocument.batchIndex(),
                plan.batchSize(),
                progress.startedAt(),
                progress.estimatedCompletion(),
                progress.lastProgressAt()
        ));

        return new PreparedProcessStep(process, plan, progress, flags, processingDocument, plan.failurePolicy());
    }

    private void closeStep(
            PreparedProcessStep preparedStep,
            DocumentAnalysis analysis,
            RuntimeException processingFailure,
            boolean leaseLost
    ) {
        if (leaseLost) {
            abortAfterLeaseLoss(preparedStep);
            return;
        }

        Instant now = clockPort.now();
        boolean failed = processingFailure != null;

        DocumentExecution closedDocument = failed
                ? toFailed(preparedStep.documentExecution(), now, processingFailure)
                : toProcessed(preparedStep.documentExecution(), now, analysis);
        documentExecutionRepository.save(closedDocument);

        ProgressSnapshot updatedProgress = updateProgress(preparedStep.progress(), failed, now);
        progressSnapshotRepository.save(updatedProgress);

        ProcessAggregate currentProcess = processRepository.findById(preparedStep.process().processId())
                .orElseThrow(() -> new IllegalStateException("Missing process during close step"));

        boolean workCompleted = documentExecutionRepository.countPendingByProcessId(currentProcess.processId()) == 0;
        boolean failureDetected = failed && preparedStep.failurePolicy() == FailurePolicy.FAIL_FAST;

        ProcessAggregate resolvedProcess = transitionEngine.apply(
                        currentProcess,
                        new ResolveCheckpoint(),
                        new TransitionContext(failureDetected, workCompleted)
                )
                .newAggregate()
                .touch(now);

        resolvedProcess = applyResultKind(resolvedProcess, updatedProgress, workCompleted);
        processRepository.save(resolvedProcess);
        processResultProjectionService.snapshotCurrent(resolvedProcess);

        activityLogRepository.save(buildDocumentActivity(closedDocument, resolvedProcess, now));
        if (closedDocument.documentStatus() == DocumentStatus.PROCESSED) {
            runtimeTelemetry.recordDocumentProcessed(resolvedProcess, closedDocument);
        } else {
            runtimeTelemetry.recordDocumentFailed(resolvedProcess, closedDocument);
        }

        if (resolvedProcess.state().isTerminal()) {
            executionControlFlagsRepository.save(new ExecutionControlFlags(
                    currentProcess.processId(),
                    false,
                    false,
                    now,
                    resolvedProcess.state().code()
            ));
            terminalInfoRepository.save(new TerminalInfo(
                    idGeneratorPort.generate(),
                    resolvedProcess.processId(),
                    resolvedProcess.state().code(),
                    now,
                    terminalReasonCode(resolvedProcess),
                    terminalReasonMessage(resolvedProcess),
                    updatedProgress.percentage()
            ));
            activityLogRepository.save(buildTerminalActivity(resolvedProcess, now));
            recordTerminalTelemetry(resolvedProcess);
        } else if ("PAUSED".equals(resolvedProcess.state().code())) {
            executionControlFlagsRepository.save(new ExecutionControlFlags(
                    currentProcess.processId(),
                    false,
                    false,
                    now,
                    "PAUSE"
            ));
            activityLogRepository.save(new ActivityLogEntry(
                    idGeneratorPort.generate(),
                    resolvedProcess.processId(),
                    now,
                    "PROCESS_PAUSED",
                    "RUNTIME",
                    "Process paused at safe document checkpoint.",
                    Map.of("status", resolvedProcess.state().code()),
                    resolvedProcess.processId()
            ));
            runtimeTelemetry.recordProcessPaused(resolvedProcess);
        }
    }

    private void resolveCheckpoint(
            ProcessAggregate process,
            ProgressSnapshot progress,
            ExecutionControlFlags flags,
            boolean workCompleted
    ) {
        Instant now = clockPort.now();
        ProcessAggregate resolvedProcess = transitionEngine.apply(
                        process,
                        new ResolveCheckpoint(),
                        new TransitionContext(false, workCompleted)
                )
                .newAggregate()
                .touch(now);
        resolvedProcess = applyResultKind(resolvedProcess, progress, workCompleted);
        processRepository.save(resolvedProcess);
        processResultProjectionService.snapshotCurrent(resolvedProcess);
        runtimeTelemetry.recordCheckpointResolved(resolvedProcess, resolvedProcess.state().code());

        if ("PAUSED".equals(resolvedProcess.state().code()) || resolvedProcess.state().isTerminal()) {
            executionControlFlagsRepository.save(new ExecutionControlFlags(
                    process.processId(),
                    false,
                    false,
                    now,
                    flags.lastControlCommandType()
            ));
        }

        if ("PAUSED".equals(resolvedProcess.state().code())) {
            activityLogRepository.save(new ActivityLogEntry(
                    idGeneratorPort.generate(),
                    resolvedProcess.processId(),
                    now,
                    "PROCESS_PAUSED",
                    "RUNTIME",
                    "Process paused at safe document checkpoint.",
                    Map.of("status", resolvedProcess.state().code()),
                    resolvedProcess.processId()
            ));
            runtimeTelemetry.recordProcessPaused(resolvedProcess);
            return;
        }

        if (resolvedProcess.state().isTerminal()) {
            terminalInfoRepository.save(new TerminalInfo(
                    idGeneratorPort.generate(),
                    resolvedProcess.processId(),
                    resolvedProcess.state().code(),
                    now,
                    terminalReasonCode(resolvedProcess),
                    terminalReasonMessage(resolvedProcess),
                    progress.percentage()
            ));
            activityLogRepository.save(buildTerminalActivity(resolvedProcess, now));
            recordTerminalTelemetry(resolvedProcess);
        }
    }

    private ProcessAggregate applyResultKind(ProcessAggregate process, ProgressSnapshot progress, boolean workCompleted) {
        Instant now = clockPort.now();
        if ("COMPLETED".equals(process.state().code())) {
            return process.withResultKind(ResultKind.FINAL, now);
        }
        if (progress.processedFiles() > 0 || progress.failedFiles() > 0) {
            return process.withResultKind(ResultKind.PARTIAL, now);
        }
        // STOPPED and FAILED legitimately remain NONE when there is no closed documentary evidence yet.
        if (workCompleted || process.state().isTerminal()) {
            return process.withResultKind(ResultKind.NONE, now);
        }
        return process;
    }

    private void abortAfterLeaseLoss(PreparedProcessStep preparedStep) {
        Instant now = clockPort.now();
        documentExecutionRepository.save(new DocumentExecution(
                preparedStep.documentExecution().documentExecutionId(),
                preparedStep.documentExecution().processId(),
                preparedStep.documentExecution().documentName(),
                preparedStep.documentExecution().documentPath(),
                DocumentStatus.PENDING,
                preparedStep.documentExecution().batchIndex(),
                null,
                null,
                null,
                null,
                null,
                preparedStep.documentExecution().mostFrequentWords(),
                null,
                null,
                null,
                null
        ));

        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                preparedStep.process().processId(),
                now,
                "LEASE_RENEWAL_FAILED",
                "RUNTIME",
                "Worker aborted current document after losing the process lease.",
                Map.of(
                        "document_name", preparedStep.documentExecution().documentName(),
                        "document_status", "PENDING"
                ),
                preparedStep.process().processId()
        ));
        ProcessAggregate refreshedProcess = preparedStep.process().touch(now);
        processRepository.save(refreshedProcess);
        processResultProjectionService.snapshotCurrent(refreshedProcess);
        runtimeTelemetry.recordWorkerAborted(
                preparedStep.process().processId(),
                preparedStep.documentExecution().documentExecutionId(),
                processLeasePort.findOwnerId(preparedStep.process().processId()).orElse(null),
                "PENDING"
        );
    }

    private LeaseRenewalSession startLeaseRenewal(String processId, String ownerId, Duration leaseDuration) {
        if (ownerId == null || leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            return LeaseRenewalSession.noop();
        }

        Duration renewalPeriod = leaseDuration.dividedBy(2);
        if (renewalPeriod.isZero()) {
            renewalPeriod = Duration.ofMillis(250);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
                .name("chronicle-lease-renewal-", 0)
                .factory());
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicBoolean leaseLost = new AtomicBoolean(false);
        Runnable renewalTask = () -> {
            if (!active.get()) {
                return;
            }
            if (!processLeasePort.renew(processId, ownerId, clockPort.now().plus(leaseDuration))) {
                leaseLost.set(true);
                active.set(false);
                runtimeTelemetry.recordLeaseRenewalFailed(processId, ownerId);
                return;
            }
            runtimeTelemetry.recordLeaseRenewed(processId, ownerId);
        };
        scheduler.scheduleAtFixedRate(
                renewalTask,
                renewalPeriod.toMillis(),
                renewalPeriod.toMillis(),
                TimeUnit.MILLISECONDS
        );
        return new LeaseRenewalSession(active, leaseLost, scheduler);
    }

    private record LeaseRenewalSession(
            AtomicBoolean active,
            AtomicBoolean leaseLost,
            ScheduledExecutorService scheduler
    ) implements AutoCloseable {

        static LeaseRenewalSession noop() {
            return new LeaseRenewalSession(null, new AtomicBoolean(false), null);
        }

        @Override
        public void close() {
            if (active != null) {
                active.set(false);
            }
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }

    }

    private DocumentExecution toProcessing(DocumentExecution documentExecution, Instant now) {
        return new DocumentExecution(
                documentExecution.documentExecutionId(),
                documentExecution.processId(),
                documentExecution.documentName(),
                documentExecution.documentPath(),
                DocumentStatus.PROCESSING,
                documentExecution.batchIndex(),
                now,
                null,
                null,
                null,
                null,
                documentExecution.mostFrequentWords(),
                null,
                null,
                null,
                null
        );
    }

    private DocumentExecution toProcessed(DocumentExecution documentExecution, Instant now, DocumentAnalysis analysis) {
        return new DocumentExecution(
                documentExecution.documentExecutionId(),
                documentExecution.processId(),
                documentExecution.documentName(),
                documentExecution.documentPath(),
                DocumentStatus.PROCESSED,
                documentExecution.batchIndex(),
                documentExecution.startedAt(),
                now,
                analysis.wordCount(),
                analysis.lineCount(),
                analysis.characterCount(),
                analysis.mostFrequentWords(),
                analysis.summary(),
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                null,
                null
        );
    }

    private DocumentExecution toFailed(DocumentExecution documentExecution, Instant now, RuntimeException failure) {
        return new DocumentExecution(
                documentExecution.documentExecutionId(),
                documentExecution.processId(),
                documentExecution.documentName(),
                documentExecution.documentPath(),
                DocumentStatus.FAILED,
                documentExecution.batchIndex(),
                documentExecution.startedAt(),
                now,
                null,
                null,
                null,
                documentExecution.mostFrequentWords(),
                null,
                null,
                "DOCUMENT_READ_ERROR",
                failure.getMessage()
        );
    }

    private ProgressSnapshot updateProgress(ProgressSnapshot progress, boolean failed, Instant now) {
        int processedFiles = progress.processedFiles() + 1;
        int successfulFiles = failed ? progress.successfulFiles() : progress.successfulFiles() + 1;
        int failedFiles = failed ? progress.failedFiles() + 1 : progress.failedFiles();
        int pendingFiles = Math.max(progress.pendingFiles() - 1, 0);
        double percentage = progress.totalFiles() == 0
                ? 0.0
                : (processedFiles * 100.0) / progress.totalFiles();

        return new ProgressSnapshot(
                progress.progressSnapshotId(),
                progress.processId(),
                progress.totalFiles(),
                processedFiles,
                successfulFiles,
                failedFiles,
                pendingFiles,
                percentage,
                progress.currentBatchIndex(),
                progress.currentBatchSize(),
                progress.startedAt(),
                progress.estimatedCompletion(),
                now
        );
    }

    private ActivityLogEntry buildDocumentActivity(DocumentExecution documentExecution, ProcessAggregate process, Instant now) {
        return new ActivityLogEntry(
                idGeneratorPort.generate(),
                process.processId(),
                now,
                documentExecution.documentStatus() == DocumentStatus.PROCESSED ? "DOCUMENT_PROCESSED" : "DOCUMENT_FAILED",
                "RUNTIME",
                documentExecution.documentStatus() == DocumentStatus.PROCESSED
                        ? "Document processed successfully."
                        : "Document processing failed.",
                Map.of(
                        "document_name", documentExecution.documentName(),
                        "document_status", documentExecution.documentStatus().name()
                ),
                process.processId()
        );
    }

    private ActivityLogEntry buildTerminalActivity(ProcessAggregate process, Instant now) {
        String eventType = switch (process.state().code()) {
            case "STOPPED" -> "PROCESS_STOPPED";
            case "COMPLETED" -> "PROCESS_COMPLETED";
            case "FAILED" -> "PROCESS_FAILED";
            default -> throw new IllegalArgumentException("Unsupported terminal state " + process.state().code());
        };

        String message = switch (process.state().code()) {
            case "STOPPED" -> "Process stopped at safe document checkpoint.";
            case "COMPLETED" -> "Process completed after processing all planned documents.";
            case "FAILED" -> "Process failed due to runtime or document processing error.";
            default -> throw new IllegalArgumentException("Unsupported terminal state " + process.state().code());
        };

        return new ActivityLogEntry(
                idGeneratorPort.generate(),
                process.processId(),
                now,
                eventType,
                "RUNTIME",
                message,
                Map.of("status", process.state().code()),
                process.processId()
        );
    }

    private String terminalReasonCode(ProcessAggregate process) {
        return switch (process.state().code()) {
            case "STOPPED" -> "MANUAL_STOP";
            case "COMPLETED" -> "PROCESS_COMPLETED";
            case "FAILED" -> "PROCESS_FAILED";
            default -> throw new IllegalArgumentException("Unsupported terminal state " + process.state().code());
        };
    }

    private String terminalReasonMessage(ProcessAggregate process) {
        return switch (process.state().code()) {
            case "STOPPED" -> "Stop command accepted and applied at safe checkpoint.";
            case "COMPLETED" -> "All planned documents were processed.";
            case "FAILED" -> "A runtime failure forced process termination.";
            default -> throw new IllegalArgumentException("Unsupported terminal state " + process.state().code());
        };
    }

    private void recordTerminalTelemetry(ProcessAggregate resolvedProcess) {
        switch (resolvedProcess.state().code()) {
            case "STOPPED" -> runtimeTelemetry.recordProcessStopped(resolvedProcess);
            case "COMPLETED" -> runtimeTelemetry.recordProcessCompleted(resolvedProcess);
            case "FAILED" -> runtimeTelemetry.recordProcessFailed(resolvedProcess);
            default -> {
            }
        }
    }
}
