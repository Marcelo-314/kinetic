package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProcessJpaRepository extends JpaRepository<ProcessJpaEntity, String> {
}
