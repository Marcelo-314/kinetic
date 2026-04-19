package com.chronicle.domain.port;

import com.chronicle.domain.model.ProcessPlan;

import java.util.Optional;

public interface ProcessPlanRepository {

    ProcessPlan save(ProcessPlan processPlan);

    Optional<ProcessPlan> findByProcessId(String processId);
}
