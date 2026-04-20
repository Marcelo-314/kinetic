package com.chronicle.domain.port;

import com.chronicle.domain.model.ActivityLogEntry;

import java.time.Instant;
import java.util.List;

public interface ActivityLogRepository {

    ActivityLogEntry save(ActivityLogEntry activityLogEntry);

    List<ActivityLogEntry> findByProcessId(String processId);

    List<ActivityLogEntry> findByProcessId(
            String processId,
            Instant from,
            Instant to,
            String eventType,
            int page,
            int pageSize
    );

    long countByProcessId(
            String processId,
            Instant from,
            Instant to,
            String eventType
    );
}
