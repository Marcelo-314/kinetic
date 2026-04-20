package com.chronicle.adapters.in.web.dto;

import java.time.Instant;

public record AuthorizationViewDto(
        boolean authorizationRequired,
        String authorizationState,
        String pendingReason,
        Instant authorizedAt
) {
}
