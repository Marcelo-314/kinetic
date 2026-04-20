package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ExecutionControlFlagsPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataExecutionControlFlagsJpaRepository;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ExecutionControlFlagsRepositoryAdapter implements ExecutionControlFlagsRepository {

    private final SpringDataExecutionControlFlagsJpaRepository repository;
    private final ExecutionControlFlagsPersistenceMapper mapper;

    public ExecutionControlFlagsRepositoryAdapter(SpringDataExecutionControlFlagsJpaRepository repository, ExecutionControlFlagsPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ExecutionControlFlags save(ExecutionControlFlags executionControlFlags) {
        return mapper.toDomain(repository.save(mapper.toEntity(executionControlFlags)));
    }

    @Override
    public Optional<ExecutionControlFlags> findByProcessId(String processId) {
        return repository.findById(processId).map(mapper::toDomain);
    }
}
