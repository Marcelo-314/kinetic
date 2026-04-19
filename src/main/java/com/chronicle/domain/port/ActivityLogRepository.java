package com.chronicle.domain.port;

import com.chronicle.domain.model.ActivityLogEntry;

import java.util.List;

public interface ActivityLogRepository {

    ActivityLogEntry save(ActivityLogEntry activityLogEntry);

    List<ActivityLogEntry> findByProcessId(String processId);
}
