package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ProcessJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataProcessJpaRepository extends JpaRepository<ProcessJpaEntity, String> {

    List<ProcessJpaEntity> findTop20ByStatusOrderByUpdatedAtAsc(String status);
}
