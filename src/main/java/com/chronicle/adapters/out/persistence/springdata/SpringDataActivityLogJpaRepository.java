package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.ActivityLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataActivityLogJpaRepository extends JpaRepository<ActivityLogJpaEntity, String> {

    List<ActivityLogJpaEntity> findByProcessIdOrderByTimestampAsc(String processId);
}
