package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.AuthorizationInfoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAuthorizationInfoJpaRepository extends JpaRepository<AuthorizationInfoJpaEntity, String> {

    Optional<AuthorizationInfoJpaEntity> findByProcessId(String processId);
}
