package com.chronicle.application.usecase;

import com.chronicle.application.request.CreateProcessRequest;
import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.exception.ProcessNotFoundException;
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
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.FileSourcePort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.TerminalInfoRepository;
import com.chronicle.domain.transition.DefaultTransitionEngine;
import com.chronicle.domain.transition.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationUseCasesTest {

    @Test
    void createProcessGeneratesInitialObjectsAndPendingAggregate() {
        TestContext ctx = new TestContext();
        CreateProcessUseCase useCase = new CreateProcessUseCase(
                ctx.processRepository,
                ctx.processPlanRepository,
                ctx.authorizationInfoRepository,
                ctx.progressSnapshotRepository,
                ctx.executionControlFlagsRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                ctx.fileSourcePort
        );

        ProcessAggregate process = useCase.execute(new CreateProcessRequest(
                "Analyze docs",
                "/data/input",
                SelectionMode.EXPLICIT_SELECTION,
                List.of("a.txt", "b.txt"),
                2,
                SummaryPolicy.EXTRACTIVE_DETERMINISTIC,
                FailurePolicy.TOLERATE_PARTIAL_FAILURES,
                true
        ));

        assertEquals("PENDING", process.state().code());
        assertEquals(1L, process.version());
        assertNotNull(ctx.processPlanRepository.findByProcessId(process.processId()).orElse(null));
        assertEquals(AuthorizationState.WAITING, ctx.authorizationInfoRepository.findByProcessId(process.processId()).orElseThrow().authorizationState());
        assertEquals(2, ctx.progressSnapshotRepository.findByProcessId(process.processId()).orElseThrow().totalFiles());
        assertFalse(ctx.executionControlFlagsRepository.findByProcessId(process.processId()).orElseThrow().pauseRequested());
        assertEquals(1, ctx.activityLogRepository.findByProcessId(process.processId()).size());
    }

    @Test
    void authorizeProcessOnlyWorksForPending() {
        TestContext ctx = new TestContext();
        String processId = ctx.seedPendingProcess();
        AuthorizeProcessUseCase useCase = new AuthorizeProcessUseCase(
                ctx.processRepository,
                ctx.authorizationInfoRepository,
                ctx.progressSnapshotRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );

        ProcessAggregate process = useCase.execute(new ProcessCommandRequest(processId));

        assertEquals("RUNNING", process.state().code());
        assertEquals(AuthorizationState.AUTHORIZED, ctx.authorizationInfoRepository.findByProcessId(processId).orElseThrow().authorizationState());
        assertNotNull(ctx.progressSnapshotRepository.findByProcessId(processId).orElseThrow().startedAt());
    }

    @Test
    void authorizeInvalidStatePropagatesDomainSemantics() {
        TestContext ctx = new TestContext();
        String processId = ctx.seedRunningProcess();
        AuthorizeProcessUseCase useCase = new AuthorizeProcessUseCase(
                ctx.processRepository,
                ctx.authorizationInfoRepository,
                ctx.progressSnapshotRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );

        assertThrows(InvalidTransitionException.class, () -> useCase.execute(new ProcessCommandRequest(processId)));
    }

    @Test
    void pauseProcessRegistersPauseIntent() {
        TestContext ctx = new TestContext();
        String processId = ctx.seedRunningProcess();
        PauseProcessUseCase useCase = new PauseProcessUseCase(
                ctx.processRepository,
                ctx.executionControlFlagsRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );

        ProcessAggregate process = useCase.execute(new ProcessCommandRequest(processId));

        assertEquals("RUNNING", process.state().code());
        assertTrue(process.pauseRequested());
        assertTrue(ctx.executionControlFlagsRepository.findByProcessId(processId).orElseThrow().pauseRequested());
    }

    @Test
    void resumeProcessOnlyWorksFromPaused() {
        TestContext ctx = new TestContext();
        String processId = ctx.seedPausedProcess();
        ResumeProcessUseCase useCase = new ResumeProcessUseCase(
                ctx.processRepository,
                ctx.executionControlFlagsRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );

        ProcessAggregate process = useCase.execute(new ProcessCommandRequest(processId));

        assertEquals("RUNNING", process.state().code());
        assertFalse(ctx.executionControlFlagsRepository.findByProcessId(processId).orElseThrow().pauseRequested());
    }

    @Test
    void stopProcessWorksForPendingAndPausedImmediatelyAndRunningAsIntent() {
        TestContext ctx = new TestContext();
        StopProcessUseCase useCase = new StopProcessUseCase(
                ctx.processRepository,
                ctx.executionControlFlagsRepository,
                ctx.terminalInfoRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );

        String pendingId = ctx.seedPendingProcess();
        ProcessAggregate stoppedPending = useCase.execute(new ProcessCommandRequest(pendingId));
        assertEquals("STOPPED", stoppedPending.state().code());
        assertEquals("MANUAL_STOP", ctx.terminalInfoRepository.findByProcessId(pendingId).orElseThrow().terminalReasonCode());

        String pausedId = ctx.seedPausedProcess();
        ProcessAggregate stoppedPaused = useCase.execute(new ProcessCommandRequest(pausedId));
        assertEquals("STOPPED", stoppedPaused.state().code());

        String runningId = ctx.seedRunningProcess();
        ProcessAggregate runningWithStopRequest = useCase.execute(new ProcessCommandRequest(runningId));
        assertEquals("RUNNING", runningWithStopRequest.state().code());
        assertTrue(ctx.executionControlFlagsRepository.findByProcessId(runningId).orElseThrow().stopRequested());
    }

    @Test
    void invalidCasesAndMissingProcessDoNotDependOnSpring() {
        TestContext ctx = new TestContext();
        ResumeProcessUseCase resumeProcessUseCase = new ResumeProcessUseCase(
                ctx.processRepository,
                ctx.executionControlFlagsRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );
        assertThrows(ProcessNotFoundException.class, () -> resumeProcessUseCase.execute(new ProcessCommandRequest("missing")));

        String pendingId = ctx.seedPendingProcess();
        PauseProcessUseCase pauseProcessUseCase = new PauseProcessUseCase(
                ctx.processRepository,
                ctx.executionControlFlagsRepository,
                ctx.activityLogRepository,
                ctx.clockPort,
                ctx.idGeneratorPort,
                new DefaultTransitionEngine()
        );
        assertThrows(InvalidTransitionException.class, () -> pauseProcessUseCase.execute(new ProcessCommandRequest(pendingId)));
    }

    private static final class TestContext {
        private final InMemoryProcessRepository processRepository = new InMemoryProcessRepository();
        private final InMemoryProcessPlanRepository processPlanRepository = new InMemoryProcessPlanRepository();
        private final InMemoryAuthorizationInfoRepository authorizationInfoRepository = new InMemoryAuthorizationInfoRepository();
        private final InMemoryProgressSnapshotRepository progressSnapshotRepository = new InMemoryProgressSnapshotRepository();
        private final InMemoryExecutionControlFlagsRepository executionControlFlagsRepository = new InMemoryExecutionControlFlagsRepository();
        private final InMemoryTerminalInfoRepository terminalInfoRepository = new InMemoryTerminalInfoRepository();
        private final InMemoryActivityLogRepository activityLogRepository = new InMemoryActivityLogRepository();
        private final ClockPort clockPort = () -> Instant.parse("2026-04-19T15:00:00Z");
        private final IdGeneratorPort idGeneratorPort = new SequenceIdGenerator();
        private final FileSourcePort fileSourcePort = new InMemoryFileSourcePort();

        String seedPendingProcess() {
            ProcessAggregate process = ProcessAggregate.pending("process-pending");
            processRepository.save(process);
            authorizationInfoRepository.save(new AuthorizationInfo("auth-pending", process.processId(), true, AuthorizationState.WAITING, "AWAITING_AUTHORIZATION", null, null, clockPort.now()));
            progressSnapshotRepository.save(new ProgressSnapshot("prog-pending", process.processId(), 2, 0, 0, 0, 2, 0.0, null, null, null, null, null));
            executionControlFlagsRepository.save(new ExecutionControlFlags(process.processId(), false, false, null, null));
            return process.processId();
        }

        String seedRunningProcess() {
            ProcessAggregate process = new ProcessAggregate("process-running", com.chronicle.domain.state.ProcessState.running(), 2L, false, false);
            processRepository.save(process);
            executionControlFlagsRepository.save(new ExecutionControlFlags(process.processId(), false, false, null, null));
            authorizationInfoRepository.save(new AuthorizationInfo("auth-running", process.processId(), true, AuthorizationState.AUTHORIZED, null, clockPort.now(), null, clockPort.now()));
            progressSnapshotRepository.save(new ProgressSnapshot("prog-running", process.processId(), 2, 0, 0, 0, 2, 0.0, null, null, clockPort.now(), null, null));
            return process.processId();
        }

        String seedPausedProcess() {
            ProcessAggregate process = new ProcessAggregate("process-paused", com.chronicle.domain.state.ProcessState.paused(), 2L, false, false);
            processRepository.save(process);
            executionControlFlagsRepository.save(new ExecutionControlFlags(process.processId(), false, false, null, null));
            return process.processId();
        }
    }

    private static final class SequenceIdGenerator implements IdGeneratorPort {
        private int value = 0;

        @Override
        public String generate() {
            value += 1;
            return "id-" + value;
        }
    }

    private static final class InMemoryFileSourcePort implements FileSourcePort {
        @Override
        public boolean folderExists(String sourceFolder) {
            return "/data/input".equals(sourceFolder);
        }

        @Override
        public List<String> listTextFiles(String sourceFolder) {
            return List.of("doc-01.txt", "doc-02.txt");
        }
    }

    private static final class InMemoryProcessRepository implements ProcessRepository {
        private final Map<String, ProcessAggregate> store = new HashMap<>();

        @Override
        public Optional<ProcessAggregate> findById(String processId) {
            return Optional.ofNullable(store.get(processId));
        }

        @Override
        public ProcessAggregate save(ProcessAggregate aggregate) {
            store.put(aggregate.processId(), aggregate);
            return aggregate;
        }
    }

    private static final class InMemoryProcessPlanRepository implements ProcessPlanRepository {
        private final Map<String, ProcessPlan> store = new HashMap<>();

        @Override
        public ProcessPlan save(ProcessPlan processPlan) {
            store.put(processPlan.processId(), processPlan);
            return processPlan;
        }

        @Override
        public Optional<ProcessPlan> findByProcessId(String processId) {
            return Optional.ofNullable(store.get(processId));
        }
    }

    private static final class InMemoryAuthorizationInfoRepository implements AuthorizationInfoRepository {
        private final Map<String, AuthorizationInfo> store = new HashMap<>();

        @Override
        public AuthorizationInfo save(AuthorizationInfo authorizationInfo) {
            store.put(authorizationInfo.processId(), authorizationInfo);
            return authorizationInfo;
        }

        @Override
        public Optional<AuthorizationInfo> findByProcessId(String processId) {
            return Optional.ofNullable(store.get(processId));
        }
    }

    private static final class InMemoryProgressSnapshotRepository implements ProgressSnapshotRepository {
        private final Map<String, ProgressSnapshot> store = new HashMap<>();

        @Override
        public ProgressSnapshot save(ProgressSnapshot progressSnapshot) {
            store.put(progressSnapshot.processId(), progressSnapshot);
            return progressSnapshot;
        }

        @Override
        public Optional<ProgressSnapshot> findByProcessId(String processId) {
            return Optional.ofNullable(store.get(processId));
        }
    }

    private static final class InMemoryExecutionControlFlagsRepository implements ExecutionControlFlagsRepository {
        private final Map<String, ExecutionControlFlags> store = new HashMap<>();

        @Override
        public ExecutionControlFlags save(ExecutionControlFlags executionControlFlags) {
            store.put(executionControlFlags.processId(), executionControlFlags);
            return executionControlFlags;
        }

        @Override
        public Optional<ExecutionControlFlags> findByProcessId(String processId) {
            return Optional.ofNullable(store.get(processId));
        }
    }

    private static final class InMemoryTerminalInfoRepository implements TerminalInfoRepository {
        private final Map<String, TerminalInfo> store = new HashMap<>();

        @Override
        public TerminalInfo save(TerminalInfo terminalInfo) {
            store.put(terminalInfo.processId(), terminalInfo);
            return terminalInfo;
        }

        @Override
        public Optional<TerminalInfo> findByProcessId(String processId) {
            return Optional.ofNullable(store.get(processId));
        }
    }

    private static final class InMemoryActivityLogRepository implements ActivityLogRepository {
        private final Map<String, List<ActivityLogEntry>> store = new HashMap<>();

        @Override
        public ActivityLogEntry save(ActivityLogEntry activityLogEntry) {
            store.computeIfAbsent(activityLogEntry.processId(), ignored -> new ArrayList<>()).add(activityLogEntry);
            return activityLogEntry;
        }

        @Override
        public List<ActivityLogEntry> findByProcessId(String processId) {
            return store.getOrDefault(processId, List.of());
        }
    }
}
