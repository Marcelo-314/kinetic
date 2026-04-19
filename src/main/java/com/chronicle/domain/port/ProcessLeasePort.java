package com.chronicle.domain.port;

import java.time.Instant;

public interface ProcessLeasePort {

    boolean tryAcquire(String processId, String ownerId, Instant leaseUntil);

    boolean renew(String processId, String ownerId, Instant leaseUntil);

    void release(String processId, String ownerId);
}
