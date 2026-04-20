package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "authorization_info")
public class AuthorizationInfoJpaEntity {

    @Id
    @Column(name = "authorization_info_id", nullable = false, updatable = false)
    private String authorizationInfoId;

    @Column(name = "process_id", nullable = false, unique = true)
    private String processId;

    @Column(name = "authorization_required", nullable = false)
    private boolean authorizationRequired;

    @Column(name = "authorization_state", nullable = false)
    private String authorizationState;

    @Column(name = "pending_reason")
    private String pendingReason;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "authorization_note")
    private String authorizationNote;

    @Column(name = "last_authorization_update_at")
    private Instant lastAuthorizationUpdateAt;

    public String getAuthorizationInfoId() {
        return authorizationInfoId;
    }

    public void setAuthorizationInfoId(String authorizationInfoId) {
        this.authorizationInfoId = authorizationInfoId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    public void setAuthorizationRequired(boolean authorizationRequired) {
        this.authorizationRequired = authorizationRequired;
    }

    public String getAuthorizationState() {
        return authorizationState;
    }

    public void setAuthorizationState(String authorizationState) {
        this.authorizationState = authorizationState;
    }

    public String getPendingReason() {
        return pendingReason;
    }

    public void setPendingReason(String pendingReason) {
        this.pendingReason = pendingReason;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public String getAuthorizationNote() {
        return authorizationNote;
    }

    public void setAuthorizationNote(String authorizationNote) {
        this.authorizationNote = authorizationNote;
    }

    public Instant getLastAuthorizationUpdateAt() {
        return lastAuthorizationUpdateAt;
    }

    public void setLastAuthorizationUpdateAt(Instant lastAuthorizationUpdateAt) {
        this.lastAuthorizationUpdateAt = lastAuthorizationUpdateAt;
    }
}
