package com.chronicle.adapters.in.web;

import com.chronicle.bootstrap.ChronicleApplication;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.TerminalInfoRepository;
import com.chronicle.domain.state.ProcessState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ChronicleApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "chronicle.runtime.scheduler.enabled=false",
                "chronicle.runtime.health.stalled-running-threshold-seconds=1"
        }
)
@AutoConfigureMockMvc
class ProcessWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    private TerminalInfoRepository terminalInfoRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    private Path sourceFolder;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM activity_log");
        jdbcTemplate.execute("DELETE FROM process_result");
        jdbcTemplate.execute("DELETE FROM process_lease");
        jdbcTemplate.execute("DELETE FROM terminal_info");
        jdbcTemplate.execute("DELETE FROM document_execution");
        jdbcTemplate.execute("DELETE FROM execution_control_flags");
        jdbcTemplate.execute("DELETE FROM progress_snapshot");
        jdbcTemplate.execute("DELETE FROM authorization_info");
        jdbcTemplate.execute("DELETE FROM process_plan");
        jdbcTemplate.execute("DELETE FROM process");

        sourceFolder = Files.createTempDirectory("chronicle-web");
        Files.writeString(sourceFolder.resolve("doc-01.txt"), "hello chronicle");
        Files.writeString(sourceFolder.resolve("doc-02.txt"), "hello api");
    }

    @Test
    void createProcessReturns201AndExpectedPayload() throws Exception {
        mockMvc.perform(post("/api/v1/processes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "objective": "Analyze docs",
                                  "source_folder": "%s",
                                  "selection_mode": "EXPLICIT_SELECTION",
                                  "selected_files": ["doc-01.txt", "doc-02.txt"],
                                  "batch_size": 1,
                                  "summary_policy": "EXTRACTIVE_DETERMINISTIC",
                                  "failure_policy": "TOLERATE_PARTIAL_FAILURES",
                                  "authorization_required": true
                                }
                                """.formatted(escapePath(sourceFolder))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.process_id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Process created and awaiting authorization."))
                .andExpect(jsonPath("$.authorization.authorization_required").value(true))
                .andExpect(jsonPath("$.links.status").exists());
    }

    @Test
    void authorizePauseResumeStopAndQueriesAreExposed() throws Exception {
        String processId = createProcess();

        mockMvc.perform(post("/api/v1/processes/{process_id}/authorize", processId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(post("/api/v1/processes/{process_id}/pause", processId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        String pausedId = seedPausedProcess();
        mockMvc.perform(post("/api/v1/processes/{process_id}/resume", pausedId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        String pendingId = createProcess();
        mockMvc.perform(post("/api/v1/processes/{process_id}/stop", pendingId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("STOPPED"));

        mockMvc.perform(get("/api/v1/processes/{process_id}/status", processId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_id").value(processId))
                .andExpect(jsonPath("$.objective").value("Analyze docs"))
                .andExpect(jsonPath("$.result_kind").value("NONE"))
                .andExpect(jsonPath("$.plan.source_folder").value(sourceFolder.toString()))
                .andExpect(jsonPath("$.progress.total_files").value(2));

        mockMvc.perform(get("/api/v1/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.page_size").value(20));
    }

    @Test
    void resultsEndpointReturnsHonestProjectionAndSupportsQueryFlags() throws Exception {
        String pendingId = createProcess();
        String stoppedNoneId = seedStoppedProcess("stopped-none", List.of(), ResultKind.NONE, false);
        String stoppedPartialId = processIdFor("stopped-partial");
        seedStoppedProcess("stopped-partial", List.of(
                processedDocument(stoppedPartialId, "doc-stop-1", "doc-01.txt", 3, 1, 20, List.of("chronicle", "runtime"), "Stopped summary")
        ), ResultKind.PARTIAL, true);
        String failedNoneId = seedFailedProcess("failed-none", List.of(), ResultKind.NONE, false);
        String failedPartialId = processIdFor("failed-partial");
        seedFailedProcess("failed-partial", List.of(
                failedDocument(failedPartialId, "doc-fail-1", "doc-02.txt", "DOCUMENT_READ_ERROR", "boom")
        ), ResultKind.PARTIAL, true);
        String completedId = processIdFor("completed");
        seedCompletedProcess();

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", pendingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_kind").value("NONE"))
                .andExpect(jsonPath("$.documents", hasSize(0)))
                .andExpect(jsonPath("$.coverage.included_files").value(0));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", stoppedNoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_status").value("STOPPED"))
                .andExpect(jsonPath("$.result_kind").value("NONE"));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", stoppedPartialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_status").value("STOPPED"))
                .andExpect(jsonPath("$.result_kind").value("PARTIAL"))
                .andExpect(jsonPath("$.documents", hasSize(1)));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", failedNoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_status").value("FAILED"))
                .andExpect(jsonPath("$.result_kind").value("NONE"));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", failedPartialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_status").value("FAILED"))
                .andExpect(jsonPath("$.result_kind").value("PARTIAL"))
                .andExpect(jsonPath("$.excluded_documents", hasSize(1)));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", completedId)
                        .queryParam("include_documents", "false")
                        .queryParam("include_global_summary", "false")
                        .queryParam("top_words_limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.process_status").value("COMPLETED"))
                .andExpect(jsonPath("$.result_kind").value("FINAL"))
                .andExpect(jsonPath("$.documents", hasSize(0)))
                .andExpect(jsonPath("$.global_summary").value(nullValue()))
                .andExpect(jsonPath("$.most_frequent_words", hasSize(1)));

        mockMvc.perform(get("/api/v1/processes/{process_id}/results", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    void activityEndpointReturnsPagedFilteredActivity() throws Exception {
        String processId = seedActivityProcess();

        mockMvc.perform(get("/api/v1/processes/{process_id}/activity", processId)
                        .queryParam("page", "1")
                        .queryParam("page_size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.page_size").value(2))
                .andExpect(jsonPath("$.total_items").value(3))
                .andExpect(jsonPath("$.items[0].event_type").value("PROCESS_PAUSED"));

        mockMvc.perform(get("/api/v1/processes/{process_id}/activity", processId)
                        .queryParam("event_type", "PROCESS_AUTHORIZED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].event_type").value("PROCESS_AUTHORIZED"));

        mockMvc.perform(get("/api/v1/processes/{process_id}/activity", processId)
                        .queryParam("from", "2026-04-19T18:01:30Z")
                        .queryParam("to", "2026-04-19T18:03:30Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));

        mockMvc.perform(get("/api/v1/processes/{process_id}/activity", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    void actuatorHealthIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void actuatorReadinessReflectsAndRecoversFromStalledRunningProcesses() throws Exception {
        String processId = "00000000-0000-0000-0000-000000009999";
        Instant createdAt = Instant.parse("2026-04-19T18:00:00Z");
        Instant staleProgressAt = Instant.now().minusSeconds(5);

        processRepository.save(new ProcessAggregate(
                processId,
                ProcessState.running(),
                2L,
                createdAt,
                createdAt,
                "Stalled process",
                ResultKind.NONE,
                false,
                false
        ));
        processPlanRepository.save(new ProcessPlan(
                "plan-stalled",
                processId,
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt"),
                1,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                createdAt
        ));
        authorizationInfoRepository.save(new AuthorizationInfo(
                "auth-stalled",
                processId,
                true,
                AuthorizationState.AUTHORIZED,
                null,
                createdAt,
                null,
                createdAt
        ));
        progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-stalled",
                processId,
                1,
                0,
                0,
                0,
                1,
                0.0,
                1,
                1,
                createdAt,
                null,
                staleProgressAt
        ));
        executionControlFlagsRepository.save(new ExecutionControlFlags(processId, false, false, createdAt, null));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.chronicleRuntime.status").value("DEGRADED"))
                .andExpect(jsonPath("$.components.chronicleRuntime.details.stalled_running_processes").value(1));

        progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-stalled",
                processId,
                1,
                0,
                0,
                0,
                1,
                0.0,
                1,
                1,
                createdAt,
                null,
                Instant.now()
        ));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.chronicleRuntime.details.stalled_running_processes").value(0));
    }

    @Test
    void errorsFollowContract() throws Exception {
        mockMvc.perform(get("/api/v1/processes/{process_id}/status", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROCESS_NOT_FOUND"));

        String processId = createProcess();
        mockMvc.perform(post("/api/v1/processes/{process_id}/pause", processId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/api/v1/processes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "objective": "",
                                  "source_folder": "%s",
                                  "selection_mode": "EXPLICIT_SELECTION",
                                  "selected_files": ["doc-01.txt"],
                                  "batch_size": 1,
                                  "summary_policy": "EXTRACTIVE_DETERMINISTIC",
                                  "failure_policy": "TOLERATE_PARTIAL_FAILURES",
                                  "authorization_required": true
                                }
                                """.formatted(escapePath(sourceFolder))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/v1/processes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "objective": "Analyze docs",
                                  "source_folder": "C:/missing/folder",
                                  "selection_mode": "EXPLICIT_SELECTION",
                                  "selected_files": ["doc-01.txt"],
                                  "batch_size": 1,
                                  "summary_policy": "EXTRACTIVE_DETERMINISTIC",
                                  "failure_policy": "TOLERATE_PARTIAL_FAILURES",
                                  "authorization_required": true
                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("SOURCE_FOLDER_NOT_FOUND"));
    }

    private String createProcess() throws Exception {
        var mvcResult = mockMvc.perform(post("/api/v1/processes")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "objective": "Analyze docs",
                                  "source_folder": "%s",
                                  "selection_mode": "EXPLICIT_SELECTION",
                                  "selected_files": ["doc-01.txt", "doc-02.txt"],
                                  "batch_size": 1,
                                  "summary_policy": "EXTRACTIVE_DETERMINISTIC",
                                  "failure_policy": "TOLERATE_PARTIAL_FAILURES",
                                  "authorization_required": true
                                }
                                """.formatted(escapePath(sourceFolder))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return json.get("process_id").asText();
    }

    private String seedPausedProcess() {
        String processId = "00000000-0000-0000-0000-000000000111";
        Instant now = Instant.parse("2026-04-19T18:00:00Z");

        processRepository.save(new ProcessAggregate(
                processId,
                ProcessState.paused(),
                2L,
                now,
                now,
                "Paused process",
                ResultKind.NONE,
                false,
                false
        ));
        processPlanRepository.save(new ProcessPlan(
                "plan-paused",
                processId,
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt"),
                1,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                now
        ));
        authorizationInfoRepository.save(new AuthorizationInfo(
                "auth-paused",
                processId,
                true,
                AuthorizationState.AUTHORIZED,
                null,
                now,
                null,
                now
        ));
        progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-paused",
                processId,
                1,
                0,
                0,
                0,
                1,
                0.0,
                1,
                1,
                now,
                null,
                now
        ));
        executionControlFlagsRepository.save(new ExecutionControlFlags(processId, false, false, now, "PAUSE"));

        return processId;
    }

    private String seedStoppedProcess(String suffix, List<DocumentExecution> documents, ResultKind resultKind, boolean withTerminalInfo) {
        return seedProcessWithDocuments(suffix, ProcessState.stopped(), documents, resultKind, withTerminalInfo);
    }

    private String seedFailedProcess(String suffix, List<DocumentExecution> documents, ResultKind resultKind, boolean withTerminalInfo) {
        return seedProcessWithDocuments(suffix, ProcessState.failed(), documents, resultKind, withTerminalInfo);
    }

    private String seedCompletedProcess() {
        return seedProcessWithDocuments(
                "completed",
                ProcessState.completed(),
                List.of(
                        processedDocument(processIdFor("completed"), "doc-complete-1", "doc-01.txt", 5, 2, 30, List.of("chronicle", "runtime"), "First summary"),
                        processedDocument(processIdFor("completed"), "doc-complete-2", "doc-02.txt", 4, 1, 25, List.of("runtime", "results"), "Second summary")
                ),
                ResultKind.FINAL,
                true
        );
    }

    private String seedProcessWithDocuments(
            String suffix,
            ProcessState state,
            List<DocumentExecution> documents,
            ResultKind resultKind,
            boolean withTerminalInfo
    ) {
        String processId = processIdFor(suffix);
        Instant now = Instant.parse("2026-04-19T18:00:00Z");
        int successfulFiles = (int) documents.stream().filter(document -> document.documentStatus() == DocumentStatus.PROCESSED).count();
        int failedFiles = (int) documents.stream().filter(document -> document.documentStatus() == DocumentStatus.FAILED).count();
        int processedFiles = successfulFiles + failedFiles;
        int totalFiles = 2;
        int pendingFiles = Math.max(totalFiles - processedFiles, 0);
        double percentage = totalFiles == 0 ? 0.0 : (processedFiles * 100.0) / totalFiles;

        processRepository.save(new ProcessAggregate(
                processId,
                state,
                2L,
                now,
                now,
                "Result projection process",
                resultKind,
                false,
                false
        ));
        processPlanRepository.save(new ProcessPlan(
                "plan-" + suffix,
                processId,
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt", "doc-02.txt"),
                totalFiles,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
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
                totalFiles,
                processedFiles,
                successfulFiles,
                failedFiles,
                pendingFiles,
                percentage,
                1,
                1,
                now,
                null,
                now
        ));
        executionControlFlagsRepository.save(new ExecutionControlFlags(processId, false, false, now, null));
        documents.forEach(documentExecutionRepository::save);
        if (withTerminalInfo) {
            terminalInfoRepository.save(new com.chronicle.domain.model.TerminalInfo(
                    "terminal-" + suffix,
                    processId,
                    state.code(),
                    now,
                    state.code() + "_TERMINAL",
                    "Terminal state recorded for results projection.",
                    percentage
            ));
        }
        return processId;
    }

    private DocumentExecution processedDocument(
            String processId,
            String documentExecutionId,
            String documentName,
            int wordCount,
            int lineCount,
            int characterCount,
            List<String> terms,
            String summary
    ) {
        return new DocumentExecution(
                documentExecutionId,
                processId,
                documentName,
                sourceFolder.resolve(documentName).toString(),
                DocumentStatus.PROCESSED,
                1,
                Instant.parse("2026-04-19T18:00:00Z"),
                Instant.parse("2026-04-19T18:01:00Z"),
                wordCount,
                lineCount,
                characterCount,
                terms.stream().map(term -> new com.chronicle.domain.model.WordFrequency(term, 2)).toList(),
                summary,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                null,
                null
        );
    }

    private DocumentExecution failedDocument(
            String processId,
            String documentExecutionId,
            String documentName,
            String errorCode,
            String errorMessage
    ) {
        return new DocumentExecution(
                documentExecutionId,
                processId,
                documentName,
                sourceFolder.resolve(documentName).toString(),
                DocumentStatus.FAILED,
                1,
                Instant.parse("2026-04-19T18:00:00Z"),
                Instant.parse("2026-04-19T18:01:00Z"),
                null,
                null,
                null,
                List.of(),
                null,
                null,
                errorCode,
                errorMessage
        );
    }

    private String processIdFor(String suffix) {
        return "00000000-0000-0000-0001-" + String.format("%012d", Math.abs(suffix.hashCode()));
    }

    private String seedActivityProcess() {
        String processId = processIdFor("activity");
        Instant now = Instant.parse("2026-04-19T18:00:00Z");
        processRepository.save(new ProcessAggregate(
                processId,
                ProcessState.running(),
                2L,
                now,
                now,
                "Activity process",
                ResultKind.PARTIAL,
                false,
                false
        ));
        processPlanRepository.save(new ProcessPlan(
                "plan-activity",
                processId,
                sourceFolder.toString(),
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt"),
                1,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                now
        ));
        authorizationInfoRepository.save(new AuthorizationInfo(
                "auth-activity",
                processId,
                true,
                AuthorizationState.AUTHORIZED,
                null,
                now,
                null,
                now
        ));
        progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-activity",
                processId,
                1,
                1,
                1,
                0,
                0,
                100.0,
                1,
                1,
                now,
                null,
                now
        ));
        executionControlFlagsRepository.save(new ExecutionControlFlags(processId, false, false, now, null));
        activityLogRepository.save(new ActivityLogEntry(
                "activity-1",
                processId,
                Instant.parse("2026-04-19T18:01:00Z"),
                "PROCESS_CREATED",
                "APPLICATION",
                "Process created and awaiting authorization.",
                java.util.Map.of("status", "PENDING"),
                processId
        ));
        activityLogRepository.save(new ActivityLogEntry(
                "activity-2",
                processId,
                Instant.parse("2026-04-19T18:02:00Z"),
                "PROCESS_AUTHORIZED",
                "APPLICATION",
                "Process authorized and dispatched.",
                java.util.Map.of("status", "RUNNING"),
                processId
        ));
        activityLogRepository.save(new ActivityLogEntry(
                "activity-3",
                processId,
                Instant.parse("2026-04-19T18:03:00Z"),
                "PROCESS_PAUSED",
                "RUNTIME",
                "Process paused at safe document checkpoint.",
                java.util.Map.of("status", "PAUSED"),
                processId
        ));
        return processId;
    }

    private String escapePath(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}
