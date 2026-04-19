package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProcessPlanJpaRepository extends JpaRepository<ProcessPlanJpaEntity, String> {

    Optional<ProcessPlanJpaEntity> findByProcessId(String processId);
}
