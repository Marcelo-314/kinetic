package com.chronicle.application.usecase;

import com.chronicle.application.query.ListProcessesQuery;
import com.chronicle.application.query.ProcessListItemView;
import com.chronicle.application.query.ProcessListView;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.ProcessRepository;

import java.util.Comparator;
import java.util.List;

public final class ListProcessesUseCase {

    private final ProcessRepository processRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;

    public ListProcessesUseCase(
            ProcessRepository processRepository,
            ProgressSnapshotRepository progressSnapshotRepository
    ) {
        this.processRepository = processRepository;
        this.progressSnapshotRepository = progressSnapshotRepository;
    }

    public ProcessListView execute(ListProcessesQuery query) {
        List<ProcessListItemView> filtered = processRepository.findAll().stream()
                .filter(process -> query.status() == null || query.status().equals(process.state().code()))
                .filter(process -> query.createdFrom() == null || !process.createdAt().isBefore(query.createdFrom()))
                .filter(process -> query.createdTo() == null || !process.createdAt().isAfter(query.createdTo()))
                .map(process -> {
                    var progress = progressSnapshotRepository.findByProcessId(process.processId())
                            .orElseThrow(() -> new IllegalStateException("Progress snapshot not found for process " + process.processId()));
                    return new ProcessListItemView(
                            process.processId(),
                            process.state().code(),
                            process.createdAt(),
                            process.updatedAt(),
                            process.resultKind(),
                            progress.totalFiles(),
                            progress.processedFiles(),
                            progress.percentage()
                    );
                })
                .sorted(comparatorFor(query.sort()))
                .toList();

        int fromIndex = Math.min((query.page() - 1) * query.pageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + query.pageSize(), filtered.size());

        return new ProcessListView(
                filtered.subList(fromIndex, toIndex),
                query.page(),
                query.pageSize(),
                filtered.size()
        );
    }

    private Comparator<ProcessListItemView> comparatorFor(String sort) {
        return switch (sort == null ? "-created_at" : sort) {
            case "created_at" -> Comparator.comparing(ProcessListItemView::createdAt);
            case "-created_at" -> Comparator.comparing(ProcessListItemView::createdAt).reversed();
            case "updated_at" -> Comparator.comparing(ProcessListItemView::updatedAt);
            case "-updated_at" -> Comparator.comparing(ProcessListItemView::updatedAt).reversed();
            default -> throw new IllegalArgumentException("Unsupported sort: " + sort);
        };
    }
}
