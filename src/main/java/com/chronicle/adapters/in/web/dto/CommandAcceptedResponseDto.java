package com.chronicle.adapters.in.web.dto;

public record CommandAcceptedResponseDto(
        String processId,
        String status,
        String message
) {
}
