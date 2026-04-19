package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ProcessJpaEntity;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.state.ProcessState;
import org.springframework.stereotype.Component;

@Component
public class ProcessPersistenceMapper {

    public ProcessJpaEntity toNewEntity(ProcessAggregate aggregate) {
        ProcessJpaEntity entity = new ProcessJpaEntity();
        entity.setProcessId(aggregate.processId());
        entity.setStatus(aggregate.state().code());
        entity.setVersion(null);
        entity.setPauseRequested(aggregate.pauseRequested());
        entity.setStopRequested(aggregate.stopRequested());
        return entity;
    }

    public ProcessJpaEntity toExistingEntity(ProcessAggregate aggregate) {
        ProcessJpaEntity entity = new ProcessJpaEntity();
        entity.setProcessId(aggregate.processId());
        entity.setStatus(aggregate.state().code());
        entity.setVersion(aggregate.version() - 2);
        entity.setPauseRequested(aggregate.pauseRequested());
        entity.setStopRequested(aggregate.stopRequested());
        return entity;
    }

    public ProcessAggregate toDomain(ProcessJpaEntity entity) {
        return new ProcessAggregate(
                entity.getProcessId(),
                toState(entity.getStatus()),
                entity.getVersion() + 1,
                entity.isPauseRequested(),
                entity.isStopRequested()
        );
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
