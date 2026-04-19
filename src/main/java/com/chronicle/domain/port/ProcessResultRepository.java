package com.chronicle.domain.port;

import com.chronicle.domain.model.ProcessResult;

import java.util.Optional;

public interface ProcessResultRepository {

    ProcessResult save(ProcessResult processResult);

    Optional<ProcessResult> findCurrentByProcessId(String processId);
}
