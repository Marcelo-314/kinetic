package com.chronicle.adapters.out.runtime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.state.ProcessState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeTelemetryTest {

    @Test
    void idleDispatchScansDoNotEmitInfoLogs() {
        RuntimeTelemetry telemetry = new RuntimeTelemetry(
                new InMemoryActivityLogRepository(),
                () -> Instant.parse("2026-04-20T12:00:00Z"),
                () -> "id-1",
                new SimpleMeterRegistry()
        );
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeTelemetry.class);
        try (LoggerCapture capture = attachAppender(logger)) {
            telemetry.recordDispatchScanStart(20);
            telemetry.recordDispatchScanEnd(0, 0);

            assertFalse(capture.appender.list.stream().anyMatch(event -> event.getLevel() == Level.INFO));
            assertTrue(capture.appender.list.stream().anyMatch(event -> event.getLevel() == Level.DEBUG));
        }
    }

    @Test
    void relevantRuntimeEventsRemainVisibleAtInfo() {
        RuntimeTelemetry telemetry = new RuntimeTelemetry(
                new InMemoryActivityLogRepository(),
                () -> Instant.parse("2026-04-20T12:00:00Z"),
                () -> "id-1",
                new SimpleMeterRegistry()
        );
        Logger logger = (Logger) LoggerFactory.getLogger(RuntimeTelemetry.class);
        ProcessAggregate process = new ProcessAggregate(
                "process-1",
                ProcessState.completed(),
                2L,
                Instant.parse("2026-04-20T11:00:00Z"),
                Instant.parse("2026-04-20T12:00:00Z"),
                "Telemetry test",
                ResultKind.FINAL,
                false,
                false
        );
        try (LoggerCapture capture = attachAppender(logger)) {
            telemetry.recordLeaseAcquired("process-1", "owner-1");
            telemetry.recordProcessCompleted(process);

            assertTrue(capture.appender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.INFO && event.getFormattedMessage().contains("lease_acquired")));
            assertTrue(capture.appender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.INFO && event.getFormattedMessage().contains("PROCESS_COMPLETED")));
        }
    }

    private LoggerCapture attachAppender(Logger logger) {
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
        return new LoggerCapture(logger, appender, previousLevel, previousAdditive);
    }

    private record LoggerCapture(
            Logger logger,
            ListAppender<ILoggingEvent> appender,
            Level previousLevel,
            boolean previousAdditive
    ) implements AutoCloseable {

        @Override
        public void close() {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
        }
    }

    private static final class InMemoryActivityLogRepository implements ActivityLogRepository {

        private final List<com.chronicle.domain.model.ActivityLogEntry> entries = new ArrayList<>();

        @Override
        public com.chronicle.domain.model.ActivityLogEntry save(com.chronicle.domain.model.ActivityLogEntry activityLogEntry) {
            entries.add(activityLogEntry);
            return activityLogEntry;
        }

        @Override
        public List<com.chronicle.domain.model.ActivityLogEntry> findByProcessId(String processId) {
            return entries.stream().filter(entry -> entry.processId().equals(processId)).toList();
        }

        @Override
        public List<com.chronicle.domain.model.ActivityLogEntry> findByProcessId(
                String processId,
                Instant from,
                Instant to,
                String eventType,
                int page,
                int pageSize
        ) {
            return findByProcessId(processId);
        }

        @Override
        public long countByProcessId(String processId, Instant from, Instant to, String eventType) {
            return findByProcessId(processId).size();
        }
    }
}
