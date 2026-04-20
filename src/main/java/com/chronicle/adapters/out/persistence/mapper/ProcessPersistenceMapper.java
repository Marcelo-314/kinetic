package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ProcessJpaEntity;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.state.ProcessState;
import org.springframework.stereotype.Component;

@Component
public class ProcessPersistenceMapper {

    public ProcessJpaEntity toNewEntity(ProcessAggregate aggregate) {
        ProcessJpaEntity entity = new ProcessJpaEntity();
        entity.setProcessId(aggregate.processId());
        entity.setStatus(aggregate.state().code());
        entity.setVersion(versionForNewEntity());
        entity.setCreatedAt(aggregate.createdAt());
        entity.setUpdatedAt(aggregate.updatedAt());
        entity.setObjective(aggregate.objective());
        entity.setResultKind(aggregate.resultKind().name());
        entity.setPauseRequested(aggregate.pauseRequested());
        entity.setStopRequested(aggregate.stopRequested());
        return entity;
    }

    public ProcessJpaEntity toExistingEntity(ProcessAggregate aggregate) {
        ProcessJpaEntity entity = new ProcessJpaEntity();
        entity.setProcessId(aggregate.processId());
        entity.setStatus(aggregate.state().code());
        entity.setVersion(expectedPersistedVersionForExistingAggregate(aggregate.version()));
        entity.setCreatedAt(aggregate.createdAt());
        entity.setUpdatedAt(aggregate.updatedAt());
        entity.setObjective(aggregate.objective());
        entity.setResultKind(aggregate.resultKind().name());
        entity.setPauseRequested(aggregate.pauseRequested());
        entity.setStopRequested(aggregate.stopRequested());
        return entity;
    }

    public ProcessAggregate toDomain(ProcessJpaEntity entity) {
        return new ProcessAggregate(
                entity.getProcessId(),
                toState(entity.getStatus()),
                toDomainVersion(entity.getVersion()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getObjective(),
                ResultKind.valueOf(entity.getResultKind()),
                entity.isPauseRequested(),
                entity.isStopRequested()
        );
    }

    private Long versionForNewEntity() {
        return null;
    }

    private long expectedPersistedVersionForExistingAggregate(long domainVersion) {
        // Domain version starts at 1, while JPA @Version starts at 0 after the first insert.
        return domainVersion - 2;
    }

    private long toDomainVersion(Long persistedVersion) {
        // The persisted version is the technical JPA value. The domain exposes the same lifecycle starting at 1.
        return persistedVersion + 1;
    }

    private ProcessState toState(String code) {
        return switch (code) {
            case "PENDING" -> ProcessState.pending();
            case "RUNNING" -> ProcessState.running();
            case "PAUSED" -> ProcessState.paused();
            case "COMPLETED" -> ProcessState.completed();
            case "FAILED" -> ProcessState.failed();
            case "STOPPED" -> ProcessState.stopped();
            default -> throw new IllegalArgumentException("Unknown process state: " + code);
        };
    }
}
