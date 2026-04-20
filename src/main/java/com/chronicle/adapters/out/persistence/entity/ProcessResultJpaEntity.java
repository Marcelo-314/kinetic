package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "process_result")
public class ProcessResultJpaEntity {

    @Id
    @Column(name = "process_result_id", nullable = false, updatable = false)
    private String processResultId;

    @Column(name = "process_id", nullable = false)
    private String processId;

    @Column(name = "result_kind", nullable = false)
    private String resultKind;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Lob
    @Column(name = "included_documents_json", nullable = false)
    private String includedDocumentsJson;

    @Lob
    @Column(name = "excluded_documents_json", nullable = false)
    private String excludedDocumentsJson;

    @Column(name = "total_words", nullable = false)
    private long totalWords;

    @Column(name = "total_lines", nullable = false)
    private long totalLines;

    @Column(name = "total_characters", nullable = false)
    private long totalCharacters;

    @Lob
    @Column(name = "most_frequent_words_json", nullable = false)
    private String mostFrequentWordsJson;

    @Lob
    @Column(name = "document_summaries_json", nullable = false)
    private String documentSummariesJson;

    @Lob
    @Column(name = "global_summary")
    private String globalSummary;

    @Column(name = "planned_files", nullable = false)
    private int plannedFiles;

    @Column(name = "included_files", nullable = false)
    private int includedFiles;

    @Column(name = "excluded_files", nullable = false)
    private int excludedFiles;

    @Column(name = "coverage_percentage", nullable = false)
    private double coveragePercentage;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    public String getProcessResultId() {
        return processResultId;
    }

    public void setProcessResultId(String processResultId) {
        this.processResultId = processResultId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getResultKind() {
        return resultKind;
    }

    public void setResultKind(String resultKind) {
        this.resultKind = resultKind;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public String getIncludedDocumentsJson() {
        return includedDocumentsJson;
    }

    public void setIncludedDocumentsJson(String includedDocumentsJson) {
        this.includedDocumentsJson = includedDocumentsJson;
    }

    public String getExcludedDocumentsJson() {
        return excludedDocumentsJson;
    }

    public void setExcludedDocumentsJson(String excludedDocumentsJson) {
        this.excludedDocumentsJson = excludedDocumentsJson;
    }

    public long getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(long totalWords) {
        this.totalWords = totalWords;
    }

    public long getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(long totalLines) {
        this.totalLines = totalLines;
    }

    public long getTotalCharacters() {
        return totalCharacters;
    }

    public void setTotalCharacters(long totalCharacters) {
        this.totalCharacters = totalCharacters;
    }

    public String getMostFrequentWordsJson() {
        return mostFrequentWordsJson;
    }

    public void setMostFrequentWordsJson(String mostFrequentWordsJson) {
        this.mostFrequentWordsJson = mostFrequentWordsJson;
    }

    public String getDocumentSummariesJson() {
        return documentSummariesJson;
    }

    public void setDocumentSummariesJson(String documentSummariesJson) {
        this.documentSummariesJson = documentSummariesJson;
    }

    public String getGlobalSummary() {
        return globalSummary;
    }

    public void setGlobalSummary(String globalSummary) {
        this.globalSummary = globalSummary;
    }

    public int getPlannedFiles() {
        return plannedFiles;
    }

    public void setPlannedFiles(int plannedFiles) {
        this.plannedFiles = plannedFiles;
    }

    public int getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(int includedFiles) {
        this.includedFiles = includedFiles;
    }

    public int getExcludedFiles() {
        return excludedFiles;
    }

    public void setExcludedFiles(int excludedFiles) {
        this.excludedFiles = excludedFiles;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}
