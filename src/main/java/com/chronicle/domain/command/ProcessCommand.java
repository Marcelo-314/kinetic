package com.chronicle.domain.command;

public sealed interface ProcessCommand permits AuthorizeProcess, PauseProcess, ResumeProcess, StopProcess, ResolveCheckpoint {
}
