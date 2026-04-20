package com.chronicle.application.query;

import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.ProgressSnapshot;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.TerminalInfo;

import java.time.Instant;

public record ProcessStatusView(
        String processId,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String objective,
        ProcessPlan plan,
        AuthorizationInfo authorization,
        ProgressSnapshot progress,
        ResultKind resultKind,
        TerminalInfo terminalInfo
) {
}
