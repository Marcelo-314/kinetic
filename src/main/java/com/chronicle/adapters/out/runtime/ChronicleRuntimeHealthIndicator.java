package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ChronicleRuntimeHealthIndicator implements HealthIndicator {

    private final DocumentExecutionRepository documentExecutionRepository;
    private final ProcessLeasePort processLeasePort;
    private final ProcessRepository processRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;
    private final ClockPort clockPort;
    private final Duration stalledRunningThreshold;

    public ChronicleRuntimeHealthIndicator(
            DocumentExecutionRepository documentExecutionRepository,
            ProcessLeasePort processLeasePort,
            ProcessRepository processRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            ClockPort clockPort,
            @Value("${chronicle.runtime.health.stalled-running-threshold-seconds:120}") long stalledRunningThresholdSeconds
    ) {
        this.documentExecutionRepository = documentExecutionRepository;
        this.processLeasePort = processLeasePort;
        this.processRepository = processRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
        this.clockPort = clockPort;
        this.stalledRunningThreshold = Duration.ofSeconds(stalledRunningThresholdSeconds);
    }

    @Override
    public Health health() {
        var now = clockPort.now();
        long orphanProcessingDocuments = documentExecutionRepository.findAllProcessing().stream()
                .filter(documentExecution -> !processLeasePort.hasActiveLease(documentExecution.processId(), now))
                .count();
        long stalledRunningProcesses = processRepository.findAll().stream()
                .filter(process -> "RUNNING".equals(process.state().code()))
                .filter(process -> !processLeasePort.hasActiveLease(process.processId(), now))
                .filter(process -> documentExecutionRepository.findProcessingByProcessId(process.processId()).isEmpty())
                .filter(process -> progressSnapshotRepository.findByProcessId(process.processId())
                        .map(progress -> progress.lastProgressAt() != null
                                && progress.lastProgressAt().plus(stalledRunningThreshold).isBefore(now))
                        .orElse(false))
                .count();

        Health.Builder builder = orphanProcessingDocuments == 0 && stalledRunningProcesses == 0
                ? Health.up()
                : Health.status("DEGRADED");
        return builder
                .withDetail("orphan_processing_documents", orphanProcessingDocuments)
                .withDetail("stalled_running_processes", stalledRunningProcesses)
                .withDetail("stalled_running_threshold_seconds", stalledRunningThreshold.toSeconds())
                .build();
    }
}
