package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessLeaseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProcessLeaseJpaRepository extends JpaRepository<ProcessLeaseJpaEntity, String> {
}
