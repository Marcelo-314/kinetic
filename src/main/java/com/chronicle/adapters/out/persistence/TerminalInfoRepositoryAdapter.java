package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.TerminalInfoPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataTerminalInfoJpaRepository;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.port.TerminalInfoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TerminalInfoRepositoryAdapter implements TerminalInfoRepository {

    private final SpringDataTerminalInfoJpaRepository repository;
    private final TerminalInfoPersistenceMapper mapper;

    public TerminalInfoRepositoryAdapter(SpringDataTerminalInfoJpaRepository repository, TerminalInfoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TerminalInfo save(TerminalInfo terminalInfo) {
        return mapper.toDomain(repository.save(mapper.toEntity(terminalInfo)));
    }

    @Override
    public Optional<TerminalInfo> findByProcessId(String processId) {
        return repository.findByProcessId(processId).map(mapper::toDomain);
    }
}
