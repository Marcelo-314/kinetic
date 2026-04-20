package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessLeaseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataProcessLeaseJpaRepository extends JpaRepository<ProcessLeaseJpaEntity, String> {

    boolean existsByProcessIdAndLeaseUntilAfter(String processId, Instant instant);

    Optional<ProcessLeaseJpaEntity> findByProcessId(String processId);
}
