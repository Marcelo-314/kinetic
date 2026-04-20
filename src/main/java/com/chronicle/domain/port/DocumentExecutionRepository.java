package com.chronicle.domain.port;

import com.chronicle.domain.model.DocumentExecution;

import java.util.List;
import java.util.Optional;

public interface DocumentExecutionRepository {

    DocumentExecution save(DocumentExecution documentExecution);

    List<DocumentExecution> findByProcessId(String processId);

    Optional<DocumentExecution> findNextPendingByProcessId(String processId);

    Optional<DocumentExecution> findProcessingByProcessId(String processId);

    long countPendingByProcessId(String processId);
}
