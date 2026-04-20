package com.chronicle.adapters.in.web.dto;

import java.util.Map;

public record ErrorResponseDto(
        ErrorBodyDto error
) {

    public static ErrorResponseDto of(String code, String message) {
        return new ErrorResponseDto(new ErrorBodyDto(code, message, Map.of()));
    }

    public static ErrorResponseDto of(String code, String message, Map<String, Object> details) {
        return new ErrorResponseDto(new ErrorBodyDto(code, message, details));
    }

    public record ErrorBodyDto(
            String code,
            String message,
            Map<String, Object> details
    ) {
    }
}
