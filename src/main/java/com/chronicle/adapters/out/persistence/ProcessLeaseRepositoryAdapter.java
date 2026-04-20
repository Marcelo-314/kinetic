package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.entity.ProcessLeaseJpaEntity;
import com.chronicle.adapters.out.persistence.springdata.SpringDataProcessLeaseJpaRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ProcessLeasePort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class ProcessLeaseRepositoryAdapter implements ProcessLeasePort {

    private final SpringDataProcessLeaseJpaRepository repository;
    private final ClockPort clockPort;

    public ProcessLeaseRepositoryAdapter(
            SpringDataProcessLeaseJpaRepository repository,
            ClockPort clockPort
    ) {
        this.repository = repository;
        this.clockPort = clockPort;
    }

    @Override
    @Transactional
    public boolean tryAcquire(String processId, String ownerId, Instant leaseUntil) {
        Instant now = clockPort.now();
        var existing = repository.findById(processId).orElse(null);
        if (existing == null) {
            ProcessLeaseJpaEntity entity = new ProcessLeaseJpaEntity();
            entity.setProcessId(processId);
            entity.setOwnerId(ownerId);
            entity.setLeaseUntil(leaseUntil);
            try {
                repository.saveAndFlush(entity);
                return true;
            } catch (DataIntegrityViolationException exception) {
                return false;
            }
        }

        if (existing.getLeaseUntil().isAfter(now) && !ownerId.equals(existing.getOwnerId())) {
            return false;
        }

        existing.setOwnerId(ownerId);
        existing.setLeaseUntil(leaseUntil);
        try {
            repository.saveAndFlush(existing);
            return true;
        } catch (ObjectOptimisticLockingFailureException exception) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean renew(String processId, String ownerId, Instant leaseUntil) {
        Instant now = clockPort.now();
        var existing = repository.findById(processId).orElse(null);
        if (existing == null) {
            return false;
        }
        if (!ownerId.equals(existing.getOwnerId()) || existing.getLeaseUntil().isBefore(now)) {
            return false;
        }

        existing.setLeaseUntil(leaseUntil);
        try {
            repository.saveAndFlush(existing);
            return true;
        } catch (ObjectOptimisticLockingFailureException exception) {
            return false;
        }
    }

    @Override
    @Transactional
    public void release(String processId, String ownerId) {
        repository.findById(processId)
                .filter(lease -> ownerId.equals(lease.getOwnerId()))
                .ifPresent(repository::delete);
    }
}
