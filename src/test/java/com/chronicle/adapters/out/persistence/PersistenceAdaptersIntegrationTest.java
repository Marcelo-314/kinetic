package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ActivityLogPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.AuthorizationInfoPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.ExecutionControlFlagsPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.ProcessPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.ProcessPlanPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.ProgressSnapshotPersistenceMapper;
import com.chronicle.adapters.out.persistence.mapper.TerminalInfoPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessJpaRepository;
import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.usecase.AuthorizeProcessUseCase;
import com.chronicle.application.usecase.CreateProcessUseCase;
import com.chronicle.bootstrap.ChronicleApplication;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.FileSourcePort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.transition.DefaultTransitionEngine;
import com.chronicle.domain.transition.TransitionEngine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = ChronicleApplication.class)
@Import({
        ProcessPersistenceMapper.class,
        ProcessPlanPersistenceMapper.class,
        AuthorizationInfoPersistenceMapper.class,
        ProgressSnapshotPersistenceMapper.class,
        ExecutionControlFlagsPersistenceMapper.class,
        TerminalInfoPersistenceMapper.class,
        ActivityLogPersistenceMapper.class,
        ProcessRepositoryAdapter.class,
        ProcessPlanRepositoryAdapter.class,
        AuthorizationInfoRepositoryAdapter.class,
        ProgressSnapshotRepositoryAdapter.class,
        ExecutionControlFlagsRepositoryAdapter.class,
        TerminalInfoRepositoryAdapter.class,
        ActivityLogRepositoryAdapter.class,
        PersistenceAdaptersIntegrationTest.TestConfig.class
})
class PersistenceAdaptersIntegrationTest {

    @Autowired
    private ProcessRepositoryAdapter processRepository;

    @Autowired
    private ProcessPlanRepositoryAdapter processPlanRepository;

    @Autowired
    private AuthorizationInfoRepositoryAdapter authorizationInfoRepository;

    @Autowired
    private ProgressSnapshotRepositoryAdapter progressSnapshotRepository;

    @Autowired
    private ExecutionControlFlagsRepositoryAdapter executionControlFlagsRepository;

    @Autowired
    private TerminalInfoRepositoryAdapter terminalInfoRepository;

    @Autowired
    private ActivityLogRepositoryAdapter activityLogRepository;

    @Autowired
    private SpringDataProcessJpaRepository springDataProcessJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CreateProcessUseCase createProcessUseCase;

    @Autowired
    private AuthorizeProcessUseCase authorizeProcessUseCase;

    @Test
    void savesAndLoadsCoreLifecycleObjects() {
        ProcessAggregate process = processRepository.save(ProcessAggregate.pending("process-1"));
        ProcessPlan plan = processPlanRepository.save(new ProcessPlan(
                "plan-1",
                process.processId(),
                "/data/input",
                SelectionMode.EXPLICIT_SELECTION,
                List.of("a.txt", "b.txt"),
                2,
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                Instant.parse("2026-04-19T18:00:00Z")
        ));
        AuthorizationInfo authorizationInfo = authorizationInfoRepository.save(new AuthorizationInfo(
                "auth-1",
                process.processId(),
                true,
                AuthorizationState.WAITING,
                "AWAITING_AUTHORIZATION",
                null,
                null,
                Instant.parse("2026-04-19T18:00:00Z")
        ));
        ProgressSnapshot progressSnapshot = progressSnapshotRepository.save(new ProgressSnapshot(
                "progress-1",
                process.processId(),
                2,
                0,
                0,
                0,
                2,
                0.0,
                null,
                null,
                null,
                null,
                null
        ));
        ExecutionControlFlags flags = executionControlFlagsRepository.save(new ExecutionControlFlags(
                process.processId(),
                false,
                false,
                null,
                null
        ));
        TerminalInfo terminalInfo = terminalInfoRepository.save(new TerminalInfo(
                "terminal-1",
                process.processId(),
                "STOPPED",
                Instant.parse("2026-04-19T18:10:00Z"),
                "MANUAL_STOP",
                "Stopped manually",
                0.0
        ));
        ActivityLogEntry activity = activityLogRepository.save(new ActivityLogEntry(
                "activity-1",
                process.processId(),
                Instant.parse("2026-04-19T18:00:00Z"),
                "PROCESS_CREATED",
                "APPLICATION",
                "Created",
                Map.of("status", "PENDING", "attempt", 1),
                "corr-1"
        ));

        assertEquals(process.processId(), processRepository.findById(process.processId()).orElseThrow().processId());
        assertEquals(plan.planId(), processPlanRepository.findByProcessId(process.processId()).orElseThrow().planId());
        assertEquals(authorizationInfo.authorizationState(), authorizationInfoRepository.findByProcessId(process.processId()).orElseThrow().authorizationState());
        assertEquals(progressSnapshot.totalFiles(), progressSnapshotRepository.findByProcessId(process.processId()).orElseThrow().totalFiles());
        assertEquals(flags.processId(), executionControlFlagsRepository.findByProcessId(process.processId()).orElseThrow().processId());
        assertEquals(terminalInfo.terminalReasonCode(), terminalInfoRepository.findByProcessId(process.processId()).orElseThrow().terminalReasonCode());
        ActivityLogEntry loadedActivity = activityLogRepository.findByProcessId(process.processId()).getFirst();
        assertEquals(activity.correlationId(), loadedActivity.correlationId());
        assertEquals("PENDING", loadedActivity.metadata().get("status"));
    }

    @Test
    void processUsesOptimisticLocking() {
        processRepository.save(ProcessAggregate.pending("process-lock"));
        springDataProcessJpaRepository.flush();
        entityManager.clear();

        ProcessAggregate firstView = processRepository.findById("process-lock").orElseThrow();
        entityManager.clear();
        ProcessAggregate staleView = processRepository.findById("process-lock").orElseThrow();

        processRepository.save(firstView.withState(com.chronicle.domain.state.ProcessState.running()));
        springDataProcessJpaRepository.flush();
        entityManager.clear();

        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> processRepository.save(staleView.withState(com.chronicle.domain.state.ProcessState.paused()))
        );
    }

    @Test
    void useCasesWorkWithRealPersistenceAdapters() {
        ProcessAggregate created = createProcessUseCase.execute(new CreateProcessRequest(
                "Analyze docs",
                "/data/input",
                SelectionMode.EXPLICIT_SELECTION,
                List.of("doc-01.txt", "doc-02.txt"),
                1,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true
        ));

        assertNotNull(processPlanRepository.findByProcessId(created.processId()).orElse(null));
        assertEquals(1L, created.version());

        ProcessAggregate running = authorizeProcessUseCase.execute(new ProcessCommandRequest(created.processId()));

        assertEquals("RUNNING", running.state().code());
        assertEquals(AuthorizationState.AUTHORIZED, authorizationInfoRepository.findByProcessId(created.processId()).orElseThrow().authorizationState());
        assertNotNull(progressSnapshotRepository.findByProcessId(created.processId()).orElseThrow().startedAt());
        assertTrue(activityLogRepository.findByProcessId(created.processId()).size() >= 2);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ClockPort clockPort() {
            return () -> Instant.parse("2026-04-19T18:00:00Z");
        }

        @Bean
        IdGeneratorPort idGeneratorPort() {
            return new IdGeneratorPort() {
                private int counter = 0;

                @Override
                public String generate() {
                    counter += 1;
                    return "id-" + counter;
                }
            };
        }

        @Bean
        FileSourcePort fileSourcePort() {
            return new FileSourcePort() {
                @Override
                public boolean folderExists(String sourceFolder) {
                    return "/data/input".equals(sourceFolder);
                }

                @Override
                public List<String> listTextFiles(String sourceFolder) {
                    return List.of("doc-01.txt", "doc-02.txt");
                }
            };
        }

        @Bean
        TransitionEngine transitionEngine() {
            return new DefaultTransitionEngine();
        }

        @Bean
        CreateProcessUseCase createProcessUseCase(
                ProcessRepositoryAdapter processRepository,
                ProcessPlanRepositoryAdapter processPlanRepository,
                AuthorizationInfoRepositoryAdapter authorizationInfoRepository,
                ProgressSnapshotRepositoryAdapter progressSnapshotRepository,
                ExecutionControlFlagsRepositoryAdapter executionControlFlagsRepository,
                ActivityLogRepositoryAdapter activityLogRepository,
                ClockPort clockPort,
                IdGeneratorPort idGeneratorPort,
                FileSourcePort fileSourcePort
        ) {
            return new CreateProcessUseCase(
                    processRepository,
                    processPlanRepository,
                    authorizationInfoRepository,
                    progressSnapshotRepository,
                    executionControlFlagsRepository,
                    activityLogRepository,
                    clockPort,
                    idGeneratorPort,
                    fileSourcePort
            );
        }

        @Bean
        AuthorizeProcessUseCase authorizeProcessUseCase(
                ProcessRepositoryAdapter processRepository,
                AuthorizationInfoRepositoryAdapter authorizationInfoRepository,
                ProgressSnapshotRepositoryAdapter progressSnapshotRepository,
                ActivityLogRepositoryAdapter activityLogRepository,
                ClockPort clockPort,
                IdGeneratorPort idGeneratorPort,
                TransitionEngine transitionEngine
        ) {
            return new AuthorizeProcessUseCase(
                    processRepository,
                    authorizationInfoRepository,
                    progressSnapshotRepository,
                    activityLogRepository,
                    clockPort,
                    idGeneratorPort,
                    transitionEngine
            );
        }
    }
}
