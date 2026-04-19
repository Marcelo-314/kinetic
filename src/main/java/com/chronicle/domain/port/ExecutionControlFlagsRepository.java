package com.chronicle.domain.port;

import com.chronicle.domain.model.ExecutionControlFlags;

import java.util.Optional;

public interface ExecutionControlFlagsRepository {

    ExecutionControlFlags save(ExecutionControlFlags executionControlFlags);

    Optional<ExecutionControlFlags> findByProcessId(String processId);
}
