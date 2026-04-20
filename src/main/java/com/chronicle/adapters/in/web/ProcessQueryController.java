package com.chronicle.adapters.in.web;

import com.chronicle.adapters.in.web.dto.ProcessListResponseDto;
import com.chronicle.adapters.in.web.dto.ProcessStatusResponseDto;
import com.chronicle.application.query.ListProcessesQuery;
import com.chronicle.application.usecase.GetProcessStatusUseCase;
import com.chronicle.application.usecase.ListProcessesUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessQueryController {

    private final GetProcessStatusUseCase getProcessStatusUseCase;
    private final ListProcessesUseCase listProcessesUseCase;
    private final ProcessWebMapper mapper;

    public ProcessQueryController(
            GetProcessStatusUseCase getProcessStatusUseCase,
            ListProcessesUseCase listProcessesUseCase,
            ProcessWebMapper mapper
    ) {
        this.getProcessStatusUseCase = getProcessStatusUseCase;
        this.listProcessesUseCase = listProcessesUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{process_id}/status")
    public ProcessStatusResponseDto getProcessStatus(@PathVariable("process_id") String processId) {
        return mapper.toStatusResponse(getProcessStatusUseCase.execute(processId));
    }

    @GetMapping
    public ProcessListResponseDto listProcesses(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "created_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(name = "created_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return mapper.toListResponse(listProcessesUseCase.execute(new ListProcessesQuery(
                status,
                createdFrom,
                createdTo,
                page,
                pageSize,
                sort
        )));
    }
}
