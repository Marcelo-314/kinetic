package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ProgressSnapshotPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProgressSnapshotJpaRepository;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProgressSnapshotRepositoryAdapter implements ProgressSnapshotRepository {

    private final SpringDataProgressSnapshotJpaRepository repository;
    private final ProgressSnapshotPersistenceMapper mapper;

    public ProgressSnapshotRepositoryAdapter(SpringDataProgressSnapshotJpaRepository repository, ProgressSnapshotPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProgressSnapshot save(ProgressSnapshot progressSnapshot) {
        return mapper.toDomain(repository.save(mapper.toEntity(progressSnapshot)));
    }

    @Override
    public Optional<ProgressSnapshot> findByProcessId(String processId) {
        return repository.findByProcessId(processId).map(mapper::toDomain);
    }
}
