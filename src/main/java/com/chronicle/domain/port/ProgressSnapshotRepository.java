package com.chronicle.domain.port;

import com.chronicle.domain.model.ProgressSnapshot;

import java.util.Optional;

public interface ProgressSnapshotRepository {

    ProgressSnapshot save(ProgressSnapshot progressSnapshot);

    Optional<ProgressSnapshot> findByProcessId(String processId);
}
