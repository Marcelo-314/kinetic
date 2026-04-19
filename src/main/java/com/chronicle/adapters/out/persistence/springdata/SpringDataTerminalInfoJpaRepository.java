package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.TerminalInfoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataTerminalInfoJpaRepository extends JpaRepository<TerminalInfoJpaEntity, String> {

    Optional<TerminalInfoJpaEntity> findByProcessId(String processId);
}
