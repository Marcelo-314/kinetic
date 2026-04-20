package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.application.service.ProcessResultProjectionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RuntimeRecoveryService {

    private final DocumentExecutionRepository documentExecutionRepository;
    private final ProcessLeasePort processLeasePort;
    private final ProcessRepository processRepository;
    private final ClockPort clockPort;
    private final ProcessResultProjectionService processResultProjectionService;
    private final RuntimeTelemetry runtimeTelemetry;

    public RuntimeRecoveryService(
            DocumentExecutionRepository documentExecutionRepository,
            ProcessLeasePort processLeasePort,
            ProcessRepository processRepository,
            ClockPort clockPort,
            ProcessResultProjectionService processResultProjectionService,
            RuntimeTelemetry runtimeTelemetry
    ) {
        this.documentExecutionRepository = documentExecutionRepository;
        this.processLeasePort = processLeasePort;
        this.processRepository = processRepository;
        this.clockPort = clockPort;
        this.processResultProjectionService = processResultProjectionService;
        this.runtimeTelemetry = runtimeTelemetry;
    }

    @Transactional
    public int reconcileOrphanProcessingDocuments() {
        int recovered = 0;
        for (DocumentExecution documentExecution : documentExecutionRepository.findAllProcessing()) {
            var now = clockPort.now();
            if (processLeasePort.hasActiveLease(documentExecution.processId(), now)) {
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
            processRepository.findById(documentExecution.processId())
                    .map(process -> process.touch(now))
                    .ifPresent(process -> {
                        processRepository.save(process);
                        processResultProjectionService.snapshotCurrent(process);
                    });
            runtimeTelemetry.recordRuntimeRecoveryApplied(documentExecution.processId(), documentExecution.documentExecutionId());
            recovered += 1;
        }
        return recovered;
    }
}
