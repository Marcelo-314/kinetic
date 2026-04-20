package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ProcessLeasePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RuntimeRecoveryService {

    private final DocumentExecutionRepository documentExecutionRepository;
    private final ProcessLeasePort processLeasePort;
    private final ClockPort clockPort;
    private final RuntimeTelemetry runtimeTelemetry;

    public RuntimeRecoveryService(
            DocumentExecutionRepository documentExecutionRepository,
            ProcessLeasePort processLeasePort,
            ClockPort clockPort,
            RuntimeTelemetry runtimeTelemetry
    ) {
        this.documentExecutionRepository = documentExecutionRepository;
        this.processLeasePort = processLeasePort;
        this.clockPort = clockPort;
        this.runtimeTelemetry = runtimeTelemetry;
    }

    @Transactional
    public int reconcileOrphanProcessingDocuments() {
        int recovered = 0;
        for (DocumentExecution documentExecution : documentExecutionRepository.findAllProcessing()) {
            if (processLeasePort.hasActiveLease(documentExecution.processId(), clockPort.now())) {
                continue;
            }
            documentExecutionRepository.save(new DocumentExecution(
                    documentExecution.documentExecutionId(),
                    documentExecution.processId(),
                    documentExecution.documentName(),
                    documentExecution.documentPath(),
                    DocumentStatus.PENDING,
                    documentExecution.batchIndex(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    documentExecution.mostFrequentWords(),
                    null,
                    null,
                    null,
                    null
            ));
            runtimeTelemetry.recordRuntimeRecoveryApplied(documentExecution.processId(), documentExecution.documentExecutionId());
            recovered += 1;
        }
        return recovered;
    }
}
