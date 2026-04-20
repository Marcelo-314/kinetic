package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.IdGeneratorPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RuntimeTelemetry {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeTelemetry.class);

    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final MeterRegistry meterRegistry;

    public RuntimeTelemetry(
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            MeterRegistry meterRegistry
    ) {
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startDispatchSample() {
        return Timer.start(meterRegistry);
    }

    public Timer.Sample startDocumentProcessingSample() {
        return Timer.start(meterRegistry);
    }

    public void recordDispatchScanStart(int scanLimit) {
        log("dispatcher_scan_start", null, null, null, null, null, null, Map.of("scan_limit", scanLimit));
    }

    public void recordDispatchScanEnd(int scanned, int dispatched) {
        log("dispatcher_scan_end", null, null, null, null, null, null, Map.of(
                "scanned_processes", scanned,
                "dispatched_processes", dispatched
        ));
    }

    public void recordDispatchDuration(Timer.Sample sample) {
        sample.stop(meterRegistry.timer("process_dispatch_duration"));
    }

    public void recordProcessDispatched(String processId, String leaseOwnerId) {
        increment("processes_dispatched_total");
        log("worker_dispatched", processId, null, null, leaseOwnerId, null, null, Map.of());
    }

    public void recordLeaseAcquired(String processId, String leaseOwnerId) {
        increment("lease_acquired_total");
        persistActivity(processId, "LEASE_ACQUIRED", "RUNTIME", "Process lease acquired.", Map.of("lease_owner_id", leaseOwnerId));
        log("lease_acquired", processId, null, null, leaseOwnerId, null, null, Map.of());
    }

    public void recordLeaseRenewed(String processId, String leaseOwnerId) {
        increment("lease_renewal_success_total");
        persistActivity(processId, "LEASE_RENEWED", "RUNTIME", "Process lease renewed.", Map.of("lease_owner_id", leaseOwnerId));
        log("lease_renewed", processId, null, null, leaseOwnerId, null, null, Map.of());
    }

    public void recordLeaseRenewalFailed(String processId, String leaseOwnerId) {
        increment("lease_renewal_failure_total");
        persistActivity(processId, "LEASE_RENEWAL_FAILED", "RUNTIME", "Process lease renewal failed.", Map.of("lease_owner_id", leaseOwnerId));
        log("lease_renewal_failed", processId, null, null, leaseOwnerId, null, null, Map.of());
    }

    public void recordWorkerStarted(String processId, String leaseOwnerId, String processStatus) {
        persistActivity(processId, "WORKER_STARTED", "RUNTIME", "Worker started for process.", metadataWithOptionalLeaseOwner(leaseOwnerId));
        log("worker_started", processId, null, null, leaseOwnerId, processStatus, null, Map.of());
    }

    public void recordWorkerAborted(String processId, String documentExecutionId, String leaseOwnerId, String documentStatus) {
        increment("worker_abort_due_to_lease_loss_total");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("document_execution_id", documentExecutionId);
        if (leaseOwnerId != null) {
            metadata.put("lease_owner_id", leaseOwnerId);
        }
        persistActivity(processId, "WORKER_ABORTED", "RUNTIME", "Worker aborted after losing lease.", metadata);
        log("worker_aborted", processId, documentExecutionId, "WORKER_ABORTED", leaseOwnerId, null, documentStatus, Map.of());
    }

    public void recordDocumentProcessingStarted(ProcessAggregate process, DocumentExecution documentExecution, String leaseOwnerId) {
        persistActivity(process.processId(), "DOCUMENT_PROCESSING_STARTED", "RUNTIME", "Document processing started.", Map.of(
                "document_execution_id", documentExecution.documentExecutionId(),
                "document_name", documentExecution.documentName()
        ));
        log("document_processing_started",
                process.processId(),
                documentExecution.documentExecutionId(),
                "DOCUMENT_PROCESSING_STARTED",
                leaseOwnerId,
                process.state().code(),
                documentExecution.documentStatus().name(),
                Map.of("correlation_id", process.processId()));
    }

    public void recordDocumentProcessed(ProcessAggregate process, DocumentExecution documentExecution) {
        increment("documents_processed_total");
        log("document_processed",
                process.processId(),
                documentExecution.documentExecutionId(),
                "DOCUMENT_PROCESSED",
                null,
                process.state().code(),
                documentExecution.documentStatus().name(),
                Map.of("correlation_id", process.processId()));
    }

    public void recordDocumentFailed(ProcessAggregate process, DocumentExecution documentExecution) {
        increment("documents_failed_total");
        log("document_failed",
                process.processId(),
                documentExecution.documentExecutionId(),
                "DOCUMENT_FAILED",
                null,
                process.state().code(),
                documentExecution.documentStatus().name(),
                Map.of("correlation_id", process.processId()));
    }

    public void recordDocumentProcessingDuration(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("document_processing_duration")
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    public void recordCheckpointResolved(ProcessAggregate process, String eventType) {
        increment("checkpoint_resolutions_total");
        persistActivity(process.processId(), "CHECKPOINT_RESOLVED", "RUNTIME", "Checkpoint resolved.", Map.of(
                "resolved_event_type", eventType,
                "process_status", process.state().code()
        ));
        log("checkpoint_resolved", process.processId(), null, "CHECKPOINT_RESOLVED", null, process.state().code(), null, Map.of(
                "resolved_event_type", eventType
        ));
    }

    public void recordProcessPaused(ProcessAggregate process) {
        increment("process_paused_total");
        log("process_paused", process.processId(), null, "PROCESS_PAUSED", null, process.state().code(), null, Map.of());
    }

    public void recordProcessStopped(ProcessAggregate process) {
        increment("process_stopped_total");
        log("process_stopped", process.processId(), null, "PROCESS_STOPPED", null, process.state().code(), null, Map.of());
    }

    public void recordProcessCompleted(ProcessAggregate process) {
        increment("process_completed_total");
        log("process_completed", process.processId(), null, "PROCESS_COMPLETED", null, process.state().code(), null, Map.of());
    }

    public void recordProcessFailed(ProcessAggregate process) {
        increment("process_failed_total");
        log("process_failed", process.processId(), null, "PROCESS_FAILED", null, process.state().code(), null, Map.of());
    }

    public void recordRuntimeRecoveryApplied(String processId, String documentExecutionId) {
        persistActivity(processId, "RUNTIME_RECOVERY_APPLIED", "RUNTIME", "Runtime recovery reset orphan PROCESSING document.", Map.of(
                "document_execution_id", documentExecutionId
        ));
        log("runtime_recovery_applied", processId, documentExecutionId, "RUNTIME_RECOVERY_APPLIED", null, null, "PENDING", Map.of());
    }

    private void increment(String counterName) {
        Counter.builder(counterName).register(meterRegistry).increment();
    }

    private void persistActivity(String processId, String eventType, String eventStage, String message, Map<String, Object> metadata) {
        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                processId,
                clockPort.now(),
                eventType,
                eventStage,
                message,
                metadata,
                processId
        ));
    }

    private Map<String, Object> metadataWithOptionalLeaseOwner(String leaseOwnerId) {
        if (leaseOwnerId == null) {
            return Map.of();
        }
        return Map.of("lease_owner_id", leaseOwnerId);
    }

    private void log(
            String eventType,
            String processId,
            String documentExecutionId,
            String activityEventType,
            String leaseOwnerId,
            String processStatus,
            String documentStatus,
            Map<String, Object> extra
    ) {
        LOGGER.info(
                "event_type={} process_id={} document_execution_id={} correlation_id={} lease_owner_id={} process_status={} document_status={} details={}",
                activityEventType == null ? eventType : activityEventType,
                processId,
                documentExecutionId,
                processId,
                leaseOwnerId,
                processStatus,
                documentStatus,
                extra
        );
    }
}
