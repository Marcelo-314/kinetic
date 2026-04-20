package com.chronicle.domain.port;

import java.time.Instant;
import java.util.Optional;

public interface ProcessLeasePort {

    boolean tryAcquire(String processId, String ownerId, Instant leaseUntil);

    boolean renew(String processId, String ownerId, Instant leaseUntil);

    void release(String processId, String ownerId);

    boolean hasActiveLease(String processId, Instant now);

    Optional<String> findOwnerId(String processId);
}
