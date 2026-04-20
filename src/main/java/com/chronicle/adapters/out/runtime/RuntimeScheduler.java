package com.chronicle.adapters.out.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "chronicle.runtime.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RuntimeScheduler {

    private final ProcessDispatcher processDispatcher;

    public RuntimeScheduler(ProcessDispatcher processDispatcher) {
        this.processDispatcher = processDispatcher;
    }

    @Scheduled(fixedDelayString = "${chronicle.runtime.dispatcher.fixed-delay-ms:1000}")
    public void schedule() {
        processDispatcher.dispatchOnce();
    }
}
