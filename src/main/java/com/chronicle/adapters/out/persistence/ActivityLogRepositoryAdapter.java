package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.ActivityLogPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataActivityLogJpaRepository;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.port.ActivityLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ActivityLogRepositoryAdapter implements ActivityLogRepository {

    private final SpringDataActivityLogJpaRepository repository;
    private final ActivityLogPersistenceMapper mapper;

    public ActivityLogRepositoryAdapter(SpringDataActivityLogJpaRepository repository, ActivityLogPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ActivityLogEntry save(ActivityLogEntry activityLogEntry) {
        return mapper.toDomain(repository.save(mapper.toEntity(activityLogEntry)));
    }

    @Override
    public List<ActivityLogEntry> findByProcessId(String processId) {
        return repository.findByProcessIdOrderByTimestampAsc(processId).stream().map(mapper::toDomain).toList();
    }
}
