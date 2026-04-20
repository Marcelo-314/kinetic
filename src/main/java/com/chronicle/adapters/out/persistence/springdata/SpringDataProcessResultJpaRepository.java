package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProcessResultJpaRepository extends JpaRepository<ProcessResultJpaEntity, String> {

    Optional<ProcessResultJpaEntity> findFirstByProcessIdOrderByComputedAtDesc(String processId);
}
