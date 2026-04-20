package com.chronicle.bootstrap;

import com.chronicle.application.usecase.AuthorizeProcessUseCase;
import com.chronicle.application.usecase.CreateProcessUseCase;
import com.chronicle.application.usecase.GetProcessActivityUseCase;
import com.chronicle.application.usecase.GetProcessResultsUseCase;
import com.chronicle.application.usecase.GetProcessStatusUseCase;
import com.chronicle.application.usecase.ListProcessesUseCase;
import com.chronicle.application.usecase.PauseProcessUseCase;
import com.chronicle.application.usecase.ResumeProcessUseCase;
import com.chronicle.application.usecase.StopProcessUseCase;
import com.chronicle.application.service.ProcessResultProjectionService;
import com.chronicle.domain.port.ActivityLogRepository;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import com.chronicle.domain.port.ClockPort;
import com.chronicle.domain.port.DocumentExecutionRepository;
import com.chronicle.domain.port.ExecutionControlFlagsRepository;
import com.chronicle.domain.port.FileSourcePort;
import com.chronicle.domain.port.IdGeneratorPort;
import com.chronicle.domain.port.ProcessPlanRepository;
import com.chronicle.domain.port.ProcessResultRepository;
import com.chronicle.domain.port.ProcessRepository;
import com.chronicle.domain.port.ProgressSnapshotRepository;
import com.chronicle.domain.port.TerminalInfoRepository;
import com.chronicle.domain.transition.DefaultTransitionEngine;
import com.chronicle.domain.transition.TransitionEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class ApplicationConfiguration {

    @Bean
    ClockPort clockPort() {
        Clock clock = Clock.systemUTC();
        return clock::instant;
    }

    @Bean
    IdGeneratorPort idGeneratorPort() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    TransitionEngine transitionEngine() {
        return new DefaultTransitionEngine();
    }

    @Bean
    ProcessResultProjectionService processResultProjectionService(
            ProcessPlanRepository processPlanRepository,
            DocumentExecutionRepository documentExecutionRepository,
            ProcessResultRepository processResultRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort
    ) {
        return new ProcessResultProjectionService(
                processPlanRepository,
                documentExecutionRepository,
                processResultRepository,
                clockPort,
                idGeneratorPort
        );
    }

    @Bean
    CreateProcessUseCase createProcessUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            FileSourcePort fileSourcePort
    ) {
        return new CreateProcessUseCase(
                processRepository,
                processPlanRepository,
                authorizationInfoRepository,
                progressSnapshotRepository,
                executionControlFlagsRepository,
                activityLogRepository,
                clockPort,
                idGeneratorPort,
                fileSourcePort
        );
    }

    @Bean
    AuthorizeProcessUseCase authorizeProcessUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            DocumentExecutionRepository documentExecutionRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        return new AuthorizeProcessUseCase(
                processRepository,
                processPlanRepository,
                authorizationInfoRepository,
                progressSnapshotRepository,
                documentExecutionRepository,
                activityLogRepository,
                clockPort,
                idGeneratorPort,
                transitionEngine
        );
    }

    @Bean
    PauseProcessUseCase pauseProcessUseCase(
            ProcessRepository processRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        return new PauseProcessUseCase(
                processRepository,
                executionControlFlagsRepository,
                activityLogRepository,
                clockPort,
                idGeneratorPort,
                transitionEngine
        );
    }

    @Bean
    ResumeProcessUseCase resumeProcessUseCase(
            ProcessRepository processRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        return new ResumeProcessUseCase(
                processRepository,
                executionControlFlagsRepository,
                activityLogRepository,
                clockPort,
                idGeneratorPort,
                transitionEngine
        );
    }

    @Bean
    StopProcessUseCase stopProcessUseCase(
            ProcessRepository processRepository,
            ExecutionControlFlagsRepository executionControlFlagsRepository,
            TerminalInfoRepository terminalInfoRepository,
            ActivityLogRepository activityLogRepository,
            ClockPort clockPort,
            IdGeneratorPort idGeneratorPort,
            TransitionEngine transitionEngine
    ) {
        return new StopProcessUseCase(
                processRepository,
                executionControlFlagsRepository,
                terminalInfoRepository,
                activityLogRepository,
                clockPort,
                idGeneratorPort,
                transitionEngine
        );
    }

    @Bean
    GetProcessStatusUseCase getProcessStatusUseCase(
            ProcessRepository processRepository,
            ProcessPlanRepository processPlanRepository,
            AuthorizationInfoRepository authorizationInfoRepository,
            ProgressSnapshotRepository progressSnapshotRepository,
            TerminalInfoRepository terminalInfoRepository
    ) {
        return new GetProcessStatusUseCase(
                processRepository,
                processPlanRepository,
                authorizationInfoRepository,
                progressSnapshotRepository,
                terminalInfoRepository
        );
    }

    @Bean
    ListProcessesUseCase listProcessesUseCase(
            ProcessRepository processRepository,
            ProgressSnapshotRepository progressSnapshotRepository
    ) {
        return new ListProcessesUseCase(processRepository, progressSnapshotRepository);
    }

    @Bean
    GetProcessResultsUseCase getProcessResultsUseCase(
            ProcessRepository processRepository,
            DocumentExecutionRepository documentExecutionRepository,
            TerminalInfoRepository terminalInfoRepository,
            ProcessResultProjectionService processResultProjectionService
    ) {
        return new GetProcessResultsUseCase(
                processRepository,
                documentExecutionRepository,
                terminalInfoRepository,
                processResultProjectionService
        );
    }

    @Bean
    GetProcessActivityUseCase getProcessActivityUseCase(
            ProcessRepository processRepository,
            ActivityLogRepository activityLogRepository
    ) {
        return new GetProcessActivityUseCase(processRepository, activityLogRepository);
    }
}
