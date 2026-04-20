package com.chronicle.adapters.out.runtime;

import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessLeasePort;
import com.chronicle.domain.port.ProcessRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Component
public class ProcessDispatcher {

    private final ProcessRepository processRepository;
    private final ProcessLeasePort processLeasePort;
    private final ProcessWorker processWorker;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final Semaphore processSlots;
    private final ExecutorService workerExecutor;
    private final Duration leaseDuration;
    private final int scanLimit;

    public ProcessDispatcher(
            ProcessRepository processRepository,
            ProcessLeasePort processLeasePort,
            ProcessWorker processWorker,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            @Value("${chronicle.runtime.max-concurrent-processes:4}") int maxConcurrentProcesses,
            @Value("${chronicle.runtime.lease-duration-seconds:30}") long leaseDurationSeconds,
            @Value("${chronicle.runtime.scan-limit:20}") int scanLimit
    ) {
        this.processRepository = processRepository;
        this.processLeasePort = processLeasePort;
        this.processWorker = processWorker;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.processSlots = new Semaphore(maxConcurrentProcesses);
        this.workerExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        this.leaseDuration = Duration.ofSeconds(leaseDurationSeconds);
        this.scanLimit = scanLimit;
    }

    public void dispatchOnce() {
        for (var process : processRepository.findRunnableProcesses(scanLimit)) {
            if (!processSlots.tryAcquire()) {
                return;
            }

            String ownerId = "worker-" + idGeneratorPort.generate();
            if (!processLeasePort.tryAcquire(process.processId(), ownerId, clockPort.now().plus(leaseDuration))) {
                processSlots.release();
                continue;
            }

            workerExecutor.submit(() -> {
                try {
                    processWorker.processNextDocument(process.processId(), ownerId, leaseDuration);
                } finally {
                    processLeasePort.release(process.processId(), ownerId);
                    processSlots.release();
                }
            });
        }
    }

    @PreDestroy
    void shutdown() {
        workerExecutor.shutdown();
    }
}
