package com.chronicle.domain.model;

import java.time.Instant;

public record TerminalInfo(
        String terminalInfoId,
        String processId,
        String terminalState,
        Instant terminalAt,
        String terminalReasonCode,
        String terminalReasonMessage,
        double finalCoverage
) {
}
