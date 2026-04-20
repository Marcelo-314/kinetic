package com.chronicle.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "process_lease")
public class ProcessLeaseJpaEntity {

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    private String processId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "lease_until", nullable = false)
    private Instant leaseUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(Instant leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
