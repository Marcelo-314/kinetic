package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ExecutionControlFlagsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataExecutionControlFlagsJpaRepository extends JpaRepository<ExecutionControlFlagsJpaEntity, String> {
}
