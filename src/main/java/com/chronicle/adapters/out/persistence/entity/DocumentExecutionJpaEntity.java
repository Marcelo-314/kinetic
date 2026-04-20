package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "document_execution")
public class DocumentExecutionJpaEntity {

    @Id
    @Column(name = "document_execution_id", nullable = false, updatable = false)
    private String documentExecutionId;

    @Column(name = "process_id", nullable = false)
    private String processId;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "document_path", nullable = false)
    private String documentPath;

    @Column(name = "document_status", nullable = false)
    private String documentStatus;

    @Column(name = "batch_index", nullable = false)
    private int batchIndex;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "line_count")
    private Integer lineCount;

    @Column(name = "character_count")
    private Integer characterCount;

    @Lob
    @Column(name = "most_frequent_words_json")
    private String mostFrequentWordsJson;

    @Lob
    @Column(name = "summary_text")
    private String summary;

    @Column(name = "summary_method")
    private String summaryMethod;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    public String getDocumentExecutionId() {
        return documentExecutionId;
    }

    public void setDocumentExecutionId(String documentExecutionId) {
        this.documentExecutionId = documentExecutionId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public int getBatchIndex() {
        return batchIndex;
    }

    public void setBatchIndex(int batchIndex) {
        this.batchIndex = batchIndex;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public String getMostFrequentWordsJson() {
        return mostFrequentWordsJson;
    }

    public void setMostFrequentWordsJson(String mostFrequentWordsJson) {
        this.mostFrequentWordsJson = mostFrequentWordsJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummaryMethod() {
        return summaryMethod;
    }

    public void setSummaryMethod(String summaryMethod) {
        this.summaryMethod = summaryMethod;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
