package com.chronicle.adapters.in.web;

import com.chronicle.bootstrap.ChronicleApplication;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ChronicleApplication.class, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
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

    private Path sourceFolder;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM activity_log");
        jdbcTemplate.execute("DELETE FROM terminal_info");
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
    void errorsFollowContract() throws Exception {
        mockMvc.perform(get("/api/v1/processes/{process_id}/status", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROCESS_NOT_FOUND"));

        String processId = createProcess();
        mockMvc.perform(post("/api/v1/processes/{process_id}/pause", processId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_TRANSITION"));

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
                .andExpect(jsonPath("$.error.code").value("SEMANTIC_VALIDATION_ERROR"));
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

    private String escapePath(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}
