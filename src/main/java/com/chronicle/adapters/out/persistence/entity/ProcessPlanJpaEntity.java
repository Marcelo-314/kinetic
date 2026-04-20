package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "process_plan")
public class ProcessPlanJpaEntity {

    @Id
    @Column(name = "plan_id", nullable = false, updatable = false)
    private String planId;

    @Column(name = "process_id", nullable = false, unique = true)
    private String processId;

    @Column(name = "source_folder", nullable = false)
    private String sourceFolder;

    @Column(name = "selection_mode", nullable = false)
    private String selectionMode;

    @Column(name = "selected_files", nullable = false, length = 4000)
    private String selectedFilesJson;

    @Column(name = "total_planned_files", nullable = false)
    private int totalPlannedFiles;

    @Column(name = "batch_size", nullable = false)
    private int batchSize;

    @Column(name = "summary_policy", nullable = false)
    private String summaryPolicy;

    @Column(name = "failure_policy", nullable = false)
    private String failurePolicy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getSourceFolder() {
        return sourceFolder;
    }

    public void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public String getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(String selectionMode) {
        this.selectionMode = selectionMode;
    }

    public String getSelectedFilesJson() {
        return selectedFilesJson;
    }

    public void setSelectedFilesJson(String selectedFilesJson) {
        this.selectedFilesJson = selectedFilesJson;
    }

    public int getTotalPlannedFiles() {
        return totalPlannedFiles;
    }

    public void setTotalPlannedFiles(int totalPlannedFiles) {
        this.totalPlannedFiles = totalPlannedFiles;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getSummaryPolicy() {
        return summaryPolicy;
    }

    public void setSummaryPolicy(String summaryPolicy) {
        this.summaryPolicy = summaryPolicy;
    }

    public String getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(String failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
