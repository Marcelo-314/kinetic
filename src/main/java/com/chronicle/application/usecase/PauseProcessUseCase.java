package com.chronicle.application.usecase;

import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.domain.command.PauseProcess;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.transition.TransitionContext;
import com.chronicle.domain.transition.TransitionEngine;

import java.util.Map;

public final class PauseProcessUseCase {

    private final ProcessRepository processRepository;
    private final ExecutionControlFlagsRepository executionControlFlagsRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final TransitionEngine transitionEngine;

    public PauseProcessUseCase(
            ProcessRepository processRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        this.processRepository = processRepository;
        this.executionControlFlagsRepository = executionControlFlagsRepository;
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.transitionEngine = transitionEngine;
    }

    public ProcessAggregate execute(ProcessCommandRequest request) {
        var now = clockPort.now();
        ProcessAggregate process = processRepository.findById(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));

        ProcessAggregate newProcess = transitionEngine.apply(process, new PauseProcess(), TransitionContext.empty())
                .newAggregate()
                .touch(now);
        processRepository.save(newProcess);
        executionControlFlagsRepository.save(new ExecutionControlFlags(
                request.processId(),
                true,
                newProcess.stopRequested(),
                now,
                "PAUSE"
        ));
        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                request.processId(),
                now,
                "PAUSE_REQUESTED",
                "APPLICATION",
                "Pause requested for running process.",
                Map.of("pause_requested", true),
                request.processId()
        ));
        return newProcess;
    }
}
