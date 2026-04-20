package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;

record PreparedProcessStep(
        ProcessAggregate process,
        ProcessPlan plan,
        ProgressSnapshot progress,
        ExecutionControlFlags flags,
        DocumentExecution documentExecution,
        FailurePolicy failurePolicy
) {

    boolean hasDocumentWork() {
        return documentExecution != null;
    }
}
