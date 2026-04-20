package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ProcessPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessJpaRepository;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.port.ProcessRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<ProcessAggregate> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProcessAggregate> findRunnableProcesses(int limit) {
        return repository.findTop20ByStatusOrderByUpdatedAtAsc("RUNNING").stream()
                .limit(limit)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public ProcessAggregate save(ProcessAggregate aggregate) {
        var currentEntity = repository.findById(aggregate.processId()).orElse(null);
        if (currentEntity == null) {
            var newEntity = mapper.toNewEntity(aggregate);
            entityManager.persist(newEntity);
            entityManager.flush();
            return mapper.toDomain(newEntity);
        }

        var currentDomain = mapper.toDomain(currentEntity);
        var entityToSave = isSameVersionMetadataUpdate(aggregate, currentDomain)
                ? mapper.toEntityUsingCurrentPersistedVersion(aggregate, currentEntity.getVersion())
                : mapper.toExistingEntity(aggregate);

        return mapper.toDomain(repository.saveAndFlush(entityToSave));
    }

    private boolean isSameVersionMetadataUpdate(ProcessAggregate candidate, ProcessAggregate current) {
        return candidate.version() == current.version()
                && candidate.state().code().equals(current.state().code())
                && candidate.pauseRequested() == current.pauseRequested()
                && candidate.stopRequested() == current.stopRequested()
                && candidate.createdAt().equals(current.createdAt())
                && candidate.objective().equals(current.objective());
    }
}
