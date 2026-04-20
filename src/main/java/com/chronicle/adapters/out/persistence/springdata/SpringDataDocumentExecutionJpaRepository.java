package com.chronicle.adapters.out.persistence.springdata;

import com.chronicle.adapters.out.persistence.entity.DocumentExecutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataDocumentExecutionJpaRepository extends JpaRepository<DocumentExecutionJpaEntity, String> {

    List<DocumentExecutionJpaEntity> findByProcessIdOrderByBatchIndexAscDocumentNameAsc(String processId);

    Optional<DocumentExecutionJpaEntity> findFirstByProcessIdAndDocumentStatusOrderByBatchIndexAscDocumentNameAsc(
            String processId,
            String documentStatus
    );

    Optional<DocumentExecutionJpaEntity> findFirstByProcessIdAndDocumentStatus(String processId, String documentStatus);

    long countByProcessIdAndDocumentStatus(String processId, String documentStatus);
}
