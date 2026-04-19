package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ProcessPlanPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessPlanJpaRepository;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.port.ProcessPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProcessPlanRepositoryAdapter implements ProcessPlanRepository {

    private final SpringDataProcessPlanJpaRepository repository;
    private final ProcessPlanPersistenceMapper mapper;

    public ProcessPlanRepositoryAdapter(SpringDataProcessPlanJpaRepository repository, ProcessPlanPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProcessPlan save(ProcessPlan processPlan) {
        return mapper.toDomain(repository.save(mapper.toEntity(processPlan)));
    }

    @Override
    public Optional<ProcessPlan> findByProcessId(String processId) {
        return repository.findByProcessId(processId).map(mapper::toDomain);
    }
}
