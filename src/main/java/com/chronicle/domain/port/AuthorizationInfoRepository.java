package com.chronicle.domain.port;

import com.chronicle.domain.model.AuthorizationInfo;

import java.util.Optional;

public interface AuthorizationInfoRepository {

    AuthorizationInfo save(AuthorizationInfo authorizationInfo);

    Optional<AuthorizationInfo> findByProcessId(String processId);
}
