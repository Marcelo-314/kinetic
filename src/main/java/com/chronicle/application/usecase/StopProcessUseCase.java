package com.chronicle.application.usecase;

import com.chronicle.application.dto.ProcessCommandRequest;
import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.domain.command.StopProcess;
import com.chronicle.domain.model.ActivityLogEntry;
import com.chronicle.domain.model.ExecutionControlFlags;
import com.chronicle.domain.model.ProcessAggregate;
import com.chronicle.domain.model.TerminalInfo;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.TerminalInfoRepository;
import com.chronicle.domain.transition.TransitionContext;
import com.chronicle.domain.transition.TransitionEngine;

public final class StopProcessUseCase {

    private final ProcessRepository processRepository;
    private final ExecutionControlFlagsRepository executionControlFlagsRepository;
    private final TerminalInfoRepository terminalInfoRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ClockPort clockPort;
    private final IdGeneratorPort idGeneratorPort;
    private final TransitionEngine transitionEngine;

    public StopProcessUseCase(
            ProcessRepository processRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            TerminalInfoRepository terminalInfoRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        this.processRepository = processRepository;
        this.executionControlFlagsRepository = executionControlFlagsRepository;
        this.terminalInfoRepository = terminalInfoRepository;
        this.activityLogRepository = activityLogRepository;
        this.clockPort = clockPort;
        this.idGeneratorPort = idGeneratorPort;
        this.transitionEngine = transitionEngine;
    }

    public ProcessAggregate execute(ProcessCommandRequest request) {
        ProcessAggregate process = processRepository.findById(request.processId())
                .orElseThrow(() -> new ProcessNotFoundException(request.processId()));

        ProcessAggregate newProcess = transitionEngine.apply(process, new StopProcess(), TransitionContext.empty()).newAggregate();
        processRepository.save(newProcess);

        if (newProcess.state().isTerminal()) {
            executionControlFlagsRepository.save(new ExecutionControlFlags(
                    request.processId(),
                    false,
                    false,
                    clockPort.now(),
                    "STOP"
            ));
            terminalInfoRepository.save(new TerminalInfo(
                    idGeneratorPort.generate(),
                    request.processId(),
                    newProcess.state().code(),
                    clockPort.now(),
                    "MANUAL_STOP",
                    "Process stopped by external command.",
                    0.0
            ));
        } else {
            executionControlFlagsRepository.save(new ExecutionControlFlags(
                    request.processId(),
                    newProcess.pauseRequested(),
                    true,
                    clockPort.now(),
                    "STOP"
            ));
        }

        activityLogRepository.save(new ActivityLogEntry(
                idGeneratorPort.generate(),
                request.processId(),
                clockPort.now(),
                newProcess.state().isTerminal() ? "PROCESS_STOPPED" : "STOP_REQUESTED",
                "APPLICATION",
                newProcess.state().isTerminal()
                        ? "Process stopped immediately."
                        : "Stop requested and waiting for checkpoint."
        ));

        return newProcess;
    }
}
