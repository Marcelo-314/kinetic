package com.chronicle.adapters.out.runtime;

import com.chronicle.bootstrap.ChronicleApplication;
import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.usecase.AuthorizeProcessUseCase;
import com.chronicle.application.usecase.CreateProcessUseCase;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = ChronicleApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "chronicle.runtime.scheduler.enabled=false",
                "chronicle.runtime.max-concurrent-processes=2",
                "chronicle.runtime.lease-duration-seconds=5"
        }
)
class RuntimeIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CreateProcessUseCase createProcessUseCase;

    @Autowired
    private AuthorizeProcessUseCase authorizeProcessUseCase;

    @Autowired
    private ProcessDispatcher processDispatcher;

    @Autowired
    private ProcessWorker processWorker;

    @Autowired
    private ProcessLeasePort processLeasePort;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProcessPlanRepository processPlanRepository;

    @Autowired
    private AuthorizationInfoRepository authorizationInfoRepository;

    @Autowired
    private ProgressSnapshotRepository progressSnapshotRepository;

    @Autowired
    private ExecutionControlFlagsRepository executionControlFlagsRepository;

    @Autowired
    private DocumentExecutionRepository documentExecutionRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ClockPort clockPort;

    private Path sourceFolder;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM activity_log");
        jdbcTemplate.execute("DELETE FROM process_lease");
        jdbcTemplate.execute("DELETE FROM terminal_info");
        jdbcTemplate.execute("DELETE FROM document_execution");
        jdbcTemplate.execute("DELETE FROM execution_control_flags");
        jdbcTemplate.execute("DELETE FROM progress_snapshot");
        jdbcTemplate.execute("DELETE FROM authorization_info");
        jdbcTemplate.execute("DELETE FROM process_plan");
        jdbcTemplate.execute("DELETE FROM process");

        sourceFolder = Files.createTempDirectory("chronicle-runtime");
        Files.writeString(sourceFolder.resolve("doc-01.txt"), "Chronicle runtime\nhandles documents cleanly.");
        Files.writeString(sourceFolder.resolve("doc-02.txt"), "Another document for deterministic processing.");
    }

    @Test
    void dispatcherDetectsRunningEligibleProcessesAndProcessesOneDocument() throws Exception {
        String processId = createAndAuthorize(List.of("doc-01.txt"));

        processDispatcher.dispatchOnce();
        waitFor(() -> progressSnapshotRepository.findByProcessId(processId).orElseThrow().processedFiles() == 1);

        ProgressSnapshot progress = progressSnapshotRepository.findByProcessId(processId).orElseThrow();
        ProcessAggregate process = processRepository.findById(processId).orElseThrow();
        List<DocumentExecution> documents = documentExecutionRepository.findByProcessId(processId);

        assertEquals(1, progress.processedFiles());
        assertEquals(0, progress.pendingFiles());
        assertEquals("COMPLETED", process.state().code());
        assertEquals(ResultKind.FINAL, process.resultKind());
        assertEquals(DocumentStatus.PROCESSED, documents.getFirst().documentStatus());
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "DOCUMENT_PROCESSED".equals(log.eventType())));
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "PROCESS_COMPLETED".equals(log.eventType())));
    }

    @Test
    void leasePreventsDoubleExecutionAndAllowsReclaimAfterExpiry() {
        String processId = UUID.randomUUID().toString();

        assertTrue(processLeasePort.tryAcquire(processId, "owner-a", clockPort.now().plusSeconds(30)));
        assertFalse(processLeasePort.tryAcquire(processId, "owner-b", clockPort.now().plusSeconds(30)));

        String expiredProcessId = UUID.randomUUID().toString();
        assertTrue(processLeasePort.tryAcquire(expiredProcessId, "owner-a", clockPort.now().minusSeconds(1)));
        assertTrue(processLeasePort.tryAcquire(expiredProcessId, "owner-b", clockPort.now().plusSeconds(30)));
    }

    @Test
    void workerProcessesOneDocumentAndUpdatesProgress() {
        String processId = createAndAuthorize(List.of("doc-01.txt", "doc-02.txt"));

        processWorker.processNextDocument(processId);

        ProgressSnapshot progress = progressSnapshotRepository.findByProcessId(processId).orElseThrow();
        ProcessAggregate process = processRepository.findById(processId).orElseThrow();

        assertEquals(1, progress.processedFiles());
        assertEquals(1, progress.pendingFiles());
        assertEquals("RUNNING", process.state().code());
        assertEquals(ResultKind.PARTIAL, process.resultKind());
    }

    @Test
    void stopRequestedPrecedesPauseRequestedAtCheckpoint() {
        String processId = seedRunningProcess(
                "stop-pause",
                List.of("doc-01.txt"),
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true,
                true,
                sourceFolder.toString()
        );

        processWorker.processNextDocument(processId);

        ProcessAggregate process = processRepository.findById(processId).orElseThrow();
        assertEquals("STOPPED", process.state().code());
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "PROCESS_STOPPED".equals(log.eventType())));
    }

    @Test
    void pauseRequestedMovesProcessToPausedAtCheckpoint() {
        String processId = seedRunningProcess(
                "pause-only",
                List.of("doc-01.txt"),
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true,
                false,
                sourceFolder.toString()
        );

        processWorker.processNextDocument(processId);

        ProcessAggregate process = processRepository.findById(processId).orElseThrow();
        assertEquals("PAUSED", process.state().code());
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "PROCESS_PAUSED".equals(log.eventType())));
    }

    @Test
    void documentFailureMarksDocumentFailedAndFailsProcessWhenPolicyIsFailFast() {
        String processId = seedRunningProcess(
                "fail-fast",
                List.of("missing.txt"),
                FailurePolicy.FAIL_FAST,
                false,
                false,
                sourceFolder.toString()
        );

        processWorker.processNextDocument(processId);

        ProcessAggregate process = processRepository.findById(processId).orElseThrow();
        DocumentExecution documentExecution = documentExecutionRepository.findByProcessId(processId).getFirst();
        ProgressSnapshot progress = progressSnapshotRepository.findByProcessId(processId).orElseThrow();

        assertEquals("FAILED", process.state().code());
        assertEquals(ResultKind.PARTIAL, process.resultKind());
        assertEquals(DocumentStatus.FAILED, documentExecution.documentStatus());
        assertEquals("DOCUMENT_READ_ERROR", documentExecution.errorCode());
        assertEquals(1, progress.failedFiles());
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "DOCUMENT_FAILED".equals(log.eventType())));
        assertTrue(activityLogRepository.findByProcessId(processId).stream().anyMatch(log -> "PROCESS_FAILED".equals(log.eventType())));
    }

    private String createAndAuthorize(List<String> selectedFiles) {
        ProcessAggregate created = createProcessUseCase.execute(new CreateProcessRequest(
                "Analyze docs",
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                selectedFiles,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true
        ));

        authorizeProcessUseCase.execute(new ProcessCommandRequest(created.processId()));
        return created.processId();
    }

    private String seedRunningProcess(
            String suffix,
            List<String> selectedFiles,
            FailurePolicy failurePolicy,
            boolean pauseRequested,
            boolean stopRequested,
            String sourceFolder
    ) {
        Instant now = clockPort.now();
        String processId = "00000000-0000-0000-0000-" + String.format("%012d", Math.abs(suffix.hashCode()));

        processRepository.save(new ProcessAggregate(
                processId,
                com.chronicle.domain.state.ProcessState.running(),
                2L,
                now,
                now,
                "Runtime seeded process",
                ResultKind.NONE,
                pauseRequested,
                stopRequested
        ));
        processPlanRepository.save(new ProcessPlan(
                "plan-" + suffix,
                processId,
                sourceFolder,
                SelectionMode.EXPLICIT_SELECTION,
                selectedFiles,
                selectedFiles.size(),
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                failurePolicy,
                now
        ));
        authorizationInfoRepository.save(new AuthorizationInfo(
                "auth-" + suffix,
                processId,
                true,
                AuthorizationState.AUTHORIZED,
                null,
                now,
                null,
                now
        ));
        progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-" + suffix,
                processId,
                selectedFiles.size(),
                0,
                0,
                0,
                selectedFiles.size(),
                0.0,
                1,
                1,
                now,
                null,
                now
        ));
        executionControlFlagsRepository.save(new ExecutionControlFlags(
                processId,
                pauseRequested,
                stopRequested,
                now,
                stopRequested ? "STOP" : (pauseRequested ? "PAUSE" : null)
        ));
        for (int index = 0; index < selectedFiles.size(); index += 1) {
            String documentName = selectedFiles.get(index);
            documentExecutionRepository.save(new DocumentExecution(
                    "doc-" + suffix + "-" + index,
                    processId,
                    documentName,
                    Path.of(sourceFolder, documentName).toString(),
                    DocumentStatus.PENDING,
                    1,
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

        return processId;
    }

    private void waitFor(Condition condition) throws Exception {
        long deadline = System.currentTimeMillis() + 3_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("Condition was not met in time");
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches() throws Exception;
    }
}
