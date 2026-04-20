package com.chronicle.adapters.in.web.dto;

import java.time.Instant;
import java.util.Map;

public record ProcessStatusResponseDto(
        String processId,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String objective,
        ProcessPlanDto plan,
        AuthorizationViewDto authorization,
        ProgressViewDto progress,
        String resultKind,
        TerminalInfoDto terminalInfo,
        Map<String, String> links
) {
}
