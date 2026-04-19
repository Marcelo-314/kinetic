package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "progress_snapshot")
public class ProgressSnapshotJpaEntity {

    @Id
    @Column(name = "progress_snapshot_id", nullable = false, updatable = false)
    private String progressSnapshotId;

    @Column(name = "process_id", nullable = false, unique = true)
    private String processId;

    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    @Column(name = "processed_files", nullable = false)
    private int processedFiles;

    @Column(name = "successful_files", nullable = false)
    private int successfulFiles;

    @Column(name = "failed_files", nullable = false)
    private int failedFiles;

    @Column(name = "pending_files", nullable = false)
    private int pendingFiles;

    @Column(name = "percentage", nullable = false)
    private double percentage;

    @Column(name = "current_batch_index")
    private Integer currentBatchIndex;

    @Column(name = "current_batch_size")
    private Integer currentBatchSize;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "estimated_completion")
    private Instant estimatedCompletion;

    @Column(name = "last_progress_at")
    private Instant lastProgressAt;

    public String getProgressSnapshotId() {
        return progressSnapshotId;
    }

    public void setProgressSnapshotId(String progressSnapshotId) {
        this.progressSnapshotId = progressSnapshotId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getProcessedFiles() {
        return processedFiles;
    }

    public void setProcessedFiles(int processedFiles) {
        this.processedFiles = processedFiles;
    }

    public int getSuccessfulFiles() {
        return successfulFiles;
    }

    public void setSuccessfulFiles(int successfulFiles) {
        this.successfulFiles = successfulFiles;
    }

    public int getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(int failedFiles) {
        this.failedFiles = failedFiles;
    }

    public int getPendingFiles() {
        return pendingFiles;
    }

    public void setPendingFiles(int pendingFiles) {
        this.pendingFiles = pendingFiles;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public Integer getCurrentBatchIndex() {
        return currentBatchIndex;
    }

    public void setCurrentBatchIndex(Integer currentBatchIndex) {
        this.currentBatchIndex = currentBatchIndex;
    }

    public Integer getCurrentBatchSize() {
        return currentBatchSize;
    }

    public void setCurrentBatchSize(Integer currentBatchSize) {
        this.currentBatchSize = currentBatchSize;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(Instant estimatedCompletion) {
        this.estimatedCompletion = estimatedCompletion;
    }

    public Instant getLastProgressAt() {
        return lastProgressAt;
    }

    public void setLastProgressAt(Instant lastProgressAt) {
        this.lastProgressAt = lastProgressAt;
    }
}
