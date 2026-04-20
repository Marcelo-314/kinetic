package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.domain.port.ClockPort;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ChronicleRuntimeHealthIndicator implements HealthIndicator {

    private final DocumentExecutionRepository documentExecutionRepository;
    private final ProcessLeasePort processLeasePort;
    private final ClockPort clockPort;

    public ChronicleRuntimeHealthIndicator(
            DocumentExecutionRepository documentExecutionRepository,
            ProcessLeasePort processLeasePort,
            ClockPort clockPort
    ) {
        this.documentExecutionRepository = documentExecutionRepository;
        this.processLeasePort = processLeasePort;
        this.clockPort = clockPort;
    }

    @Override
    public Health health() {
        long orphanProcessingDocuments = documentExecutionRepository.findAllProcessing().stream()
                .filter(documentExecution -> !processLeasePort.hasActiveLease(documentExecution.processId(), clockPort.now()))
                .count();

        Health.Builder builder = orphanProcessingDocuments == 0 ? Health.up() : Health.status("DEGRADED");
        return builder
                .withDetail("orphan_processing_documents", orphanProcessingDocuments)
                .build();
    }
}
