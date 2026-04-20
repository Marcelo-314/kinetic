package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "terminal_info")
public class TerminalInfoJpaEntity {

    @Id
    @Column(name = "terminal_info_id", nullable = false, updatable = false)
    private String terminalInfoId;

    @Column(name = "process_id", nullable = false, unique = true)
    private String processId;

    @Column(name = "terminal_state", nullable = false)
    private String terminalState;

    @Column(name = "terminal_at", nullable = false)
    private Instant terminalAt;

    @Column(name = "terminal_reason_code", nullable = false)
    private String terminalReasonCode;

    @Column(name = "terminal_reason_message")
    private String terminalReasonMessage;

    @Column(name = "final_coverage", nullable = false)
    private double finalCoverage;

    public String getTerminalInfoId() {
        return terminalInfoId;
    }

    public void setTerminalInfoId(String terminalInfoId) {
        this.terminalInfoId = terminalInfoId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getTerminalState() {
        return terminalState;
    }

    public void setTerminalState(String terminalState) {
        this.terminalState = terminalState;
    }

    public Instant getTerminalAt() {
        return terminalAt;
    }

    public void setTerminalAt(Instant terminalAt) {
        this.terminalAt = terminalAt;
    }

    public String getTerminalReasonCode() {
        return terminalReasonCode;
    }

    public void setTerminalReasonCode(String terminalReasonCode) {
        this.terminalReasonCode = terminalReasonCode;
    }

    public String getTerminalReasonMessage() {
        return terminalReasonMessage;
    }

    public void setTerminalReasonMessage(String terminalReasonMessage) {
        this.terminalReasonMessage = terminalReasonMessage;
    }

    public double getFinalCoverage() {
        return finalCoverage;
    }

    public void setFinalCoverage(double finalCoverage) {
        this.finalCoverage = finalCoverage;
    }
}
