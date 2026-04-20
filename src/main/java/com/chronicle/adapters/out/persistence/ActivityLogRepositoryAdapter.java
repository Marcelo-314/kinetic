package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.entity.ActivityLogJpaEntity;
import com.chronicle.adapters.out.persistence.mapper.ActivityLogPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataActivityLogJpaRepository;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.port.ActivityLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
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

    @Override
    public List<ActivityLogEntry> findByProcessId(
            String processId,
            Instant from,
            Instant to,
            String eventType,
            int page,
            int pageSize
    ) {
        return repository.findAll(
                        specification(processId, from, to, eventType),
                        PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("activityId")))
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByProcessId(String processId, Instant from, Instant to, String eventType) {
        return repository.count(specification(processId, from, to, eventType));
    }

    private Specification<ActivityLogJpaEntity> specification(String processId, Instant from, Instant to, String eventType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("processId"), processId));
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            if (eventType != null && !eventType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
