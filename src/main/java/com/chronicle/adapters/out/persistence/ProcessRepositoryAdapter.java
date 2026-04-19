package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ProcessPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessJpaRepository;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.port.ProcessRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class ProcessRepositoryAdapter implements ProcessRepository {

    private final SpringDataProcessJpaRepository repository;
    private final ProcessPersistenceMapper mapper;
    private final EntityManager entityManager;

    public ProcessRepositoryAdapter(
            SpringDataProcessJpaRepository repository,
            ProcessPersistenceMapper mapper,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<ProcessAggregate> findById(String processId) {
        return repository.findById(processId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ProcessAggregate save(ProcessAggregate aggregate) {
        if (!repository.existsById(aggregate.processId())) {
            var newEntity = mapper.toNewEntity(aggregate);
            entityManager.persist(newEntity);
            entityManager.flush();
            return mapper.toDomain(newEntity);
        }

        return mapper.toDomain(repository.saveAndFlush(mapper.toExistingEntity(aggregate)));
    }
}
