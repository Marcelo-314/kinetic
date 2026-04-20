package com.chronicle.application.query;

import java.util.List;

public record ProcessListView(
        List<ProcessListItemView> items,
        int page,
        int pageSize,
        long totalItems
) {
}
