package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.DocumentExecutionPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataDocumentExecutionJpaRepository;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.port.DocumentExecutionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DocumentExecutionRepositoryAdapter implements DocumentExecutionRepository {

    private final SpringDataDocumentExecutionJpaRepository repository;
    private final DocumentExecutionPersistenceMapper mapper;

    public DocumentExecutionRepositoryAdapter(
            SpringDataDocumentExecutionJpaRepository repository,
            DocumentExecutionPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public DocumentExecution save(DocumentExecution documentExecution) {
        return mapper.toDomain(repository.save(mapper.toEntity(documentExecution)));
    }

    @Override
    public List<DocumentExecution> findByProcessId(String processId) {
        return repository.findByProcessIdOrderByBatchIndexAscDocumentNameAsc(processId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DocumentExecution> findNextPendingByProcessId(String processId) {
        return repository.findFirstByProcessIdAndDocumentStatusOrderByBatchIndexAscDocumentNameAsc(processId, "PENDING")
                .map(mapper::toDomain);
    }

    @Override
    public Optional<DocumentExecution> findProcessingByProcessId(String processId) {
        return repository.findFirstByProcessIdAndDocumentStatus(processId, "PROCESSING")
                .map(mapper::toDomain);
    }

    @Override
    public long countPendingByProcessId(String processId) {
        return repository.countByProcessIdAndDocumentStatus(processId, "PENDING");
    }

    @Override
    public List<DocumentExecution> findAllProcessing() {
        return repository.findByDocumentStatusOrderByProcessIdAscBatchIndexAscDocumentNameAsc("PROCESSING")
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
