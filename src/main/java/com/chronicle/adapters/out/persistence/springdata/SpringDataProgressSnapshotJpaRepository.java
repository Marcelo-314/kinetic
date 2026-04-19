package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProgressSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProgressSnapshotJpaRepository extends JpaRepository<ProgressSnapshotJpaEntity, String> {

    Optional<ProgressSnapshotJpaEntity> findByProcessId(String processId);
}
