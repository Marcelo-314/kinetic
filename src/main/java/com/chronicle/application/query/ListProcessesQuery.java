package com.chronicle.application.query;

import java.time.Instant;

public record ListProcessesQuery(
        String status,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int pageSize,
        String sort
) {

    public ListProcessesQuery {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to one");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than or equal to one");
        }
    }
}
