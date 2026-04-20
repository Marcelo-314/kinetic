package com.chronicle.application.usecase;

import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.application.query.ActivityItemView;
import com.chronicle.application.query.GetProcessActivityQuery;
import com.chronicle.application.query.ProcessActivityView;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ProcessRepository;

public final class GetProcessActivityUseCase {

    private final ProcessRepository processRepository;
    private final ActivityLogRepository activityLogRepository;

    public GetProcessActivityUseCase(
            ProcessRepository processRepository,
            ActivityLogRepository activityLogRepository
    ) {
        this.processRepository = processRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public ProcessActivityView execute(GetProcessActivityQuery query) {
        processRepository.findById(query.processId())
                .orElseThrow(() -> new ProcessNotFoundException(query.processId()));

        var items = activityLogRepository.findByProcessId(
                        query.processId(),
                        query.from(),
                        query.to(),
                        query.eventType(),
                        query.page(),
                        query.pageSize()
                ).stream()
                .map(entry -> new ActivityItemView(
                        entry.activityId(),
                        entry.timestamp(),
                        entry.eventType(),
                        entry.eventStage(),
                        entry.message(),
                        entry.metadata()
                ))
                .toList();

        long totalItems = activityLogRepository.countByProcessId(
                query.processId(),
                query.from(),
                query.to(),
                query.eventType()
        );

        return new ProcessActivityView(items, query.page(), query.pageSize(), totalItems);
    }
}
