package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ProgressSnapshotJpaEntity;
import com.chronicle.domain.model.ProgressSnapshot;
import org.springframework.stereotype.Component;

@Component
public class ProgressSnapshotPersistenceMapper {

    public ProgressSnapshotJpaEntity toEntity(ProgressSnapshot domain) {
        ProgressSnapshotJpaEntity entity = new ProgressSnapshotJpaEntity();
        entity.setProgressSnapshotId(domain.progressSnapshotId());
        entity.setProcessId(domain.processId());
        entity.setTotalFiles(domain.totalFiles());
        entity.setProcessedFiles(domain.processedFiles());
        entity.setSuccessfulFiles(domain.successfulFiles());
        entity.setFailedFiles(domain.failedFiles());
        entity.setPendingFiles(domain.pendingFiles());
        entity.setPercentage(domain.percentage());
        entity.setCurrentBatchIndex(domain.currentBatchIndex());
        entity.setCurrentBatchSize(domain.currentBatchSize());
        entity.setStartedAt(domain.startedAt());
        entity.setEstimatedCompletion(domain.estimatedCompletion());
        entity.setLastProgressAt(domain.lastProgressAt());
        return entity;
    }

    public ProgressSnapshot toDomain(ProgressSnapshotJpaEntity entity) {
        return new ProgressSnapshot(
                entity.getProgressSnapshotId(),
                entity.getProcessId(),
                entity.getTotalFiles(),
                entity.getProcessedFiles(),
                entity.getSuccessfulFiles(),
                entity.getFailedFiles(),
                entity.getPendingFiles(),
                entity.getPercentage(),
                entity.getCurrentBatchIndex(),
                entity.getCurrentBatchSize(),
                entity.getStartedAt(),
                entity.getEstimatedCompletion(),
                entity.getLastProgressAt()
        );
    }
}
