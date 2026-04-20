package com.chronicle.domain.model;

import java.time.Instant;

public record AuthorizationInfo(
        String authorizationInfoId,
        String processId,
        boolean authorizationRequired,
        AuthorizationState authorizationState,
        String pendingReason,
        Instant authorizedAt,
        String authorizationNote,
        Instant lastAuthorizationUpdateAt
) {
}
