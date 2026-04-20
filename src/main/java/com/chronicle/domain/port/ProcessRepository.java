package com.chronicle.domain.port;

import com.chronicle.domain.model.ProcessAggregate;

import java.util.List;
import java.util.Optional;

public interface ProcessRepository {

    Optional<ProcessAggregate> findById(String processId);

    List<ProcessAggregate> findAll();

    ProcessAggregate save(ProcessAggregate aggregate);
}
