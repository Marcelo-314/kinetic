package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ExecutionControlFlagsJpaEntity;
import com.chronicle.domain.model.ExecutionControlFlags;
import org.springframework.stereotype.Component;

@Component
public class ExecutionControlFlagsPersistenceMapper {

    public ExecutionControlFlagsJpaEntity toEntity(ExecutionControlFlags domain) {
        ExecutionControlFlagsJpaEntity entity = new ExecutionControlFlagsJpaEntity();
        entity.setProcessId(domain.processId());
        entity.setPauseRequested(domain.pauseRequested());
        entity.setStopRequested(domain.stopRequested());
        entity.setLastControlCommandAt(domain.lastControlCommandAt());
        entity.setLastControlCommandType(domain.lastControlCommandType());
        return entity;
    }

    public ExecutionControlFlags toDomain(ExecutionControlFlagsJpaEntity entity) {
        return new ExecutionControlFlags(
                entity.getProcessId(),
                entity.isPauseRequested(),
                entity.isStopRequested(),
                entity.getLastControlCommandAt(),
                entity.getLastControlCommandType()
        );
    }
}
