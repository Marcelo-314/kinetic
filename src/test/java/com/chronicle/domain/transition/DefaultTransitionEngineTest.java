package com.chronicle.domain.transition;

import com.chronicle.domain.command.AuthorizeProcess;
import com.chronicle.domain.command.PauseProcess;
import com.chronicle.domain.command.ResolveCheckpoint;
import com.chronicle.domain.command.ResumeProcess;
import com.chronicle.domain.command.StopProcess;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.state.ProcessState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTransitionEngineTest {

    private TransitionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DefaultTransitionEngine();
    }

    @Test
    void authorizeIsValidFromPending() {
        ProcessAggregate process = ProcessAggregate.pending("p-1");

        TransitionResult result = engine.apply(process, new AuthorizeProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Running.class, result.newAggregate().state());
        assertFalse(result.newAggregate().pauseRequested());
        assertFalse(result.newAggregate().stopRequested());
    }

    @ParameterizedTest
    @MethodSource("nonPendingProcesses")
    void authorizeIsInvalidOutsidePending(ProcessAggregate process) {
        assertThrows(
                InvalidTransitionException.class,
                () -> engine.apply(process, new AuthorizeProcess(), TransitionContext.empty())
        );
    }

    @Test
    void pauseIsAcceptedFromRunningByRegisteringIntent() {
        ProcessAggregate process = running("p-2");

        TransitionResult result = engine.apply(process, new PauseProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Running.class, result.newAggregate().state());
        assertTrue(result.newAggregate().pauseRequested());
        assertFalse(result.newAggregate().stopRequested());
    }

    @ParameterizedTest
    @MethodSource("nonRunningProcesses")
    void pauseIsInvalidOutsideRunning(ProcessAggregate process) {
        assertThrows(
                InvalidTransitionException.class,
                () -> engine.apply(process, new PauseProcess(), TransitionContext.empty())
        );
    }

    @Test
    void resumeIsValidFromPaused() {
        ProcessAggregate process = paused("p-3");

        TransitionResult result = engine.apply(process, new ResumeProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Running.class, result.newAggregate().state());
        assertFalse(result.newAggregate().pauseRequested());
        assertFalse(result.newAggregate().stopRequested());
    }

    @ParameterizedTest
    @MethodSource("nonPausedProcesses")
    void resumeIsInvalidOutsidePaused(ProcessAggregate process) {
        assertThrows(
                InvalidTransitionException.class,
                () -> engine.apply(process, new ResumeProcess(), TransitionContext.empty())
        );
    }

    @Test
    void stopIsValidFromPending() {
        ProcessAggregate process = ProcessAggregate.pending("p-4");

        TransitionResult result = engine.apply(process, new StopProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Stopped.class, result.newAggregate().state());
    }

    @Test
    void stopIsAcceptedFromRunningByRegisteringIntent() {
        ProcessAggregate process = running("p-5");

        TransitionResult result = engine.apply(process, new StopProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Running.class, result.newAggregate().state());
        assertTrue(result.newAggregate().stopRequested());
    }

    @Test
    void stopIsValidFromPaused() {
        ProcessAggregate process = paused("p-6");

        TransitionResult result = engine.apply(process, new StopProcess(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Stopped.class, result.newAggregate().state());
    }

    @ParameterizedTest
    @MethodSource("terminalProcesses")
    void stopIsInvalidFromTerminalStates(ProcessAggregate process) {
        assertThrows(
                InvalidTransitionException.class,
                () -> engine.apply(process, new StopProcess(), TransitionContext.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("terminalProcesses")
    void terminalStatesAreAbsorbent(ProcessAggregate process) {
        assertThrows(
                InvalidTransitionException.class,
                () -> engine.apply(process, new ResolveCheckpoint(), TransitionContext.empty())
        );
    }

    @Test
    void stopPrecedesPauseAtCheckpointResolution() {
        ProcessAggregate runningWithBothSignals = running("p-7");
        ProcessAggregate afterPause = engine.apply(runningWithBothSignals, new PauseProcess(), TransitionContext.empty()).newAggregate();
        ProcessAggregate afterStop = engine.apply(afterPause, new StopProcess(), TransitionContext.empty()).newAggregate();

        TransitionResult result = engine.apply(afterStop, new ResolveCheckpoint(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Stopped.class, result.newAggregate().state());
    }

    @Test
    void failurePrecedesStoppedCompletedAndPausedAtCheckpointResolution() {
        ProcessAggregate process = running("p-8");
        process = engine.apply(process, new PauseProcess(), TransitionContext.empty()).newAggregate();
        process = engine.apply(process, new StopProcess(), TransitionContext.empty()).newAggregate();

        TransitionResult result = engine.apply(process, new ResolveCheckpoint(), new TransitionContext(true, true));

        assertInstanceOf(com.chronicle.domain.state.Failed.class, result.newAggregate().state());
    }

    @Test
    void stoppedPrecedesCompletedAndPausedAtCheckpointResolution() {
        ProcessAggregate process = running("p-9");
        process = engine.apply(process, new PauseProcess(), TransitionContext.empty()).newAggregate();
        process = engine.apply(process, new StopProcess(), TransitionContext.empty()).newAggregate();

        TransitionResult result = engine.apply(process, new ResolveCheckpoint(), TransitionContext.withWorkCompleted());

        assertInstanceOf(com.chronicle.domain.state.Stopped.class, result.newAggregate().state());
    }

    @Test
    void completedPrecedesPausedAtCheckpointResolution() {
        ProcessAggregate process = running("p-10");
        process = engine.apply(process, new PauseProcess(), TransitionContext.empty()).newAggregate();

        TransitionResult result = engine.apply(process, new ResolveCheckpoint(), TransitionContext.withWorkCompleted());

        assertInstanceOf(com.chronicle.domain.state.Completed.class, result.newAggregate().state());
    }

    @Test
    void pausedIsResolvedWhenNoHigherPriorityConditionExists() {
        ProcessAggregate process = running("p-11");
        process = engine.apply(process, new PauseProcess(), TransitionContext.empty()).newAggregate();

        TransitionResult result = engine.apply(process, new ResolveCheckpoint(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Paused.class, result.newAggregate().state());
    }

    @Test
    void runningRemainsRunningWhenCheckpointHasNothingToResolve() {
        ProcessAggregate process = running("p-12");

        TransitionResult result = engine.apply(process, new ResolveCheckpoint(), TransitionContext.empty());

        assertInstanceOf(com.chronicle.domain.state.Running.class, result.newAggregate().state());
        assertEquals(process, result.newAggregate());
    }

    private static Stream<ProcessAggregate> nonPendingProcesses() {
        return Stream.of(
                running("r-1"),
                paused("p-1"),
                completed("c-1"),
                failed("f-1"),
                stopped("s-1")
        );
    }

    private static Stream<ProcessAggregate> nonRunningProcesses() {
        return Stream.of(
                ProcessAggregate.pending("p-13"),
                paused("p-14"),
                completed("p-15"),
                failed("p-16"),
                stopped("p-17")
        );
    }

    private static Stream<ProcessAggregate> nonPausedProcesses() {
        return Stream.of(
                ProcessAggregate.pending("p-18"),
                running("p-19"),
                completed("p-20"),
                failed("p-21"),
                stopped("p-22")
        );
    }

    private static Stream<ProcessAggregate> terminalProcesses() {
        return Stream.of(
                completed("t-1"),
                failed("t-2"),
                stopped("t-3")
        );
    }

    private static ProcessAggregate running(String processId) {
        return new ProcessAggregate(processId, ProcessState.running(), 0L, false, false);
    }

    private static ProcessAggregate paused(String processId) {
        return new ProcessAggregate(processId, ProcessState.paused(), 0L, false, false);
    }

    private static ProcessAggregate completed(String processId) {
        return new ProcessAggregate(processId, ProcessState.completed(), 0L, false, false);
    }

    private static ProcessAggregate failed(String processId) {
        return new ProcessAggregate(processId, ProcessState.failed(), 0L, false, false);
    }

    private static ProcessAggregate stopped(String processId) {
        return new ProcessAggregate(processId, ProcessState.stopped(), 0L, false, false);
    }
}
