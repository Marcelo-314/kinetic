package com.chronicle.adapters.in.web;

import com.chronicle.adapters.in.web.dto.CommandAcceptedResponseDto;
import com.chronicle.adapters.in.web.dto.CreateProcessRequestDto;
import com.chronicle.adapters.in.web.dto.CreateProcessResponseDto;
import com.chronicle.application.request.ProcessCommandRequest;
import com.chronicle.application.usecase.AuthorizeProcessUseCase;
import com.chronicle.application.usecase.CreateProcessUseCase;
import com.chronicle.application.usecase.GetProcessStatusUseCase;
import com.chronicle.application.usecase.PauseProcessUseCase;
import com.chronicle.application.usecase.ResumeProcessUseCase;
import com.chronicle.application.usecase.StopProcessUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessCommandController {

    private final CreateProcessUseCase createProcessUseCase;
    private final AuthorizeProcessUseCase authorizeProcessUseCase;
    private final PauseProcessUseCase pauseProcessUseCase;
    private final ResumeProcessUseCase resumeProcessUseCase;
    private final StopProcessUseCase stopProcessUseCase;
    private final GetProcessStatusUseCase getProcessStatusUseCase;
    private final ProcessWebMapper mapper;

    public ProcessCommandController(
            CreateProcessUseCase createProcessUseCase,
            AuthorizeProcessUseCase authorizeProcessUseCase,
            PauseProcessUseCase pauseProcessUseCase,
            ResumeProcessUseCase resumeProcessUseCase,
            StopProcessUseCase stopProcessUseCase,
            GetProcessStatusUseCase getProcessStatusUseCase,
            ProcessWebMapper mapper
    ) {
        this.createProcessUseCase = createProcessUseCase;
        this.authorizeProcessUseCase = authorizeProcessUseCase;
        this.pauseProcessUseCase = pauseProcessUseCase;
        this.resumeProcessUseCase = resumeProcessUseCase;
        this.stopProcessUseCase = stopProcessUseCase;
        this.getProcessStatusUseCase = getProcessStatusUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProcessResponseDto createProcess(@Valid @RequestBody CreateProcessRequestDto request) {
        var process = createProcessUseCase.execute(mapper.toApplicationRequest(request));
        return mapper.toCreateResponse(getProcessStatusUseCase.execute(process.processId()));
    }

    @PostMapping("/{process_id}/authorize")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandAcceptedResponseDto authorizeProcess(@PathVariable("process_id") String processId) {
        return mapper.toAcceptedResponse(
                authorizeProcessUseCase.execute(new ProcessCommandRequest(processId)),
                "Authorization accepted."
        );
    }

    @PostMapping("/{process_id}/pause")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandAcceptedResponseDto pauseProcess(@PathVariable("process_id") String processId) {
        return mapper.toAcceptedResponse(
                pauseProcessUseCase.execute(new ProcessCommandRequest(processId)),
                "Pause accepted."
        );
    }

    @PostMapping("/{process_id}/resume")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandAcceptedResponseDto resumeProcess(@PathVariable("process_id") String processId) {
        return mapper.toAcceptedResponse(
                resumeProcessUseCase.execute(new ProcessCommandRequest(processId)),
                "Resume accepted."
        );
    }

    @PostMapping("/{process_id}/stop")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommandAcceptedResponseDto stopProcess(@PathVariable("process_id") String processId) {
        return mapper.toAcceptedResponse(
                stopProcessUseCase.execute(new ProcessCommandRequest(processId)),
                "Stop accepted."
        );
    }
}
