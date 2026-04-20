package com.chronicle.adapters.in.web.dto;

import java.util.Map;

public record CreateProcessResponseDto(
        String processId,
        String status,
        String message,
        AuthorizationViewDto authorization,
        Map<String, String> links
) {
}
