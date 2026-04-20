package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "execution_control_flags")
public class ExecutionControlFlagsJpaEntity {

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    private String processId;

    @Column(name = "pause_requested", nullable = false)
    private boolean pauseRequested;

    @Column(name = "stop_requested", nullable = false)
    private boolean stopRequested;

    @Column(name = "last_control_command_at")
    private Instant lastControlCommandAt;

    @Column(name = "last_control_command_type")
    private String lastControlCommandType;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public boolean isPauseRequested() {
        return pauseRequested;
    }

    public void setPauseRequested(boolean pauseRequested) {
        this.pauseRequested = pauseRequested;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void setStopRequested(boolean stopRequested) {
        this.stopRequested = stopRequested;
    }

    public Instant getLastControlCommandAt() {
        return lastControlCommandAt;
    }

    public void setLastControlCommandAt(Instant lastControlCommandAt) {
        this.lastControlCommandAt = lastControlCommandAt;
    }

    public String getLastControlCommandType() {
        return lastControlCommandType;
    }

    public void setLastControlCommandType(String lastControlCommandType) {
        this.lastControlCommandType = lastControlCommandType;
    }
}
