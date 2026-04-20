package com.chronicle.adapters.in.web.dto;

import java.time.Instant;

public record TerminalInfoDto(
        String terminalState,
        Instant terminalAt,
        String terminalReasonCode,
        String terminalReasonMessage
) {
}
