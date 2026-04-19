package com.chronicle.domain.port;

import com.chronicle.domain.model.DocumentExecution;

import java.util.List;

public interface DocumentExecutionRepository {

    DocumentExecution save(DocumentExecution documentExecution);

    List<DocumentExecution> findByProcessId(String processId);
}
