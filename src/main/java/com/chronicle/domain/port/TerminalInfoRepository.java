package com.chronicle.domain.port;

import com.chronicle.domain.model.TerminalInfo;

import java.util.Optional;

public interface TerminalInfoRepository {

    TerminalInfo save(TerminalInfo terminalInfo);

    Optional<TerminalInfo> findByProcessId(String processId);
}
