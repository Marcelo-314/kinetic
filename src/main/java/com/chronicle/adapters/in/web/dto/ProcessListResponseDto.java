package com.chronicle.adapters.in.web.dto;

import java.util.List;

public record ProcessListResponseDto(
        List<ProcessListItemDto> items,
        int page,
        int pageSize,
        long totalItems
) {
}
