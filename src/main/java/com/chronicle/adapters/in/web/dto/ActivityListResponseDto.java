package com.chronicle.adapters.in.web.dto;

import java.util.List;

public record ActivityListResponseDto(
        List<ActivityItemDto> items,
        int page,
        int pageSize,
        long totalItems
) {
}
