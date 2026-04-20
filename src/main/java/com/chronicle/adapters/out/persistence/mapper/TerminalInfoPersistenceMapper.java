package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.TerminalInfoJpaEntity;
import com.chronicle.domain.model.TerminalInfo;
import org.springframework.stereotype.Component;

@Component
public class TerminalInfoPersistenceMapper {

    public TerminalInfoJpaEntity toEntity(TerminalInfo domain) {
        TerminalInfoJpaEntity entity = new TerminalInfoJpaEntity();
        entity.setTerminalInfoId(domain.terminalInfoId());
        entity.setProcessId(domain.processId());
        entity.setTerminalState(domain.terminalState());
        entity.setTerminalAt(domain.terminalAt());
        entity.setTerminalReasonCode(domain.terminalReasonCode());
        entity.setTerminalReasonMessage(domain.terminalReasonMessage());
        entity.setFinalCoverage(domain.finalCoverage());
        return entity;
    }

    public TerminalInfo toDomain(TerminalInfoJpaEntity entity) {
        return new TerminalInfo(
                entity.getTerminalInfoId(),
                entity.getProcessId(),
                entity.getTerminalState(),
                entity.getTerminalAt(),
                entity.getTerminalReasonCode(),
                entity.getTerminalReasonMessage(),
                entity.getFinalCoverage()
        );
    }
}
