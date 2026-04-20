package com.chronicle.application.query;

import java.util.List;

public record ProcessActivityView(
        List<ActivityItemView> items,
        int page,
        int pageSize,
        long totalItems
) {
}
