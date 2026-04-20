package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ProcessResultPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessResultJpaRepository;
import com.chronicle.domain.model.ProcessResult;
import com.chronicle.domain.port.ProcessResultRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProcessResultRepositoryAdapter implements ProcessResultRepository {

    private final SpringDataProcessResultJpaRepository repository;
    private final ProcessResultPersistenceMapper mapper;

    public ProcessResultRepositoryAdapter(
            SpringDataProcessResultJpaRepository repository,
            ProcessResultPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProcessResult save(ProcessResult processResult) {
        return mapper.toDomain(repository.save(mapper.toEntity(processResult)));
    }

    @Override
    public Optional<ProcessResult> findCurrentByProcessId(String processId) {
        return repository.findFirstByProcessIdOrderByComputedAtDesc(processId).map(mapper::toDomain);
    }
}
