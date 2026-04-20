package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.AuthorizationInfoJpaEntity;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.model.AuthorizationState;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationInfoPersistenceMapper {

    public AuthorizationInfoJpaEntity toEntity(AuthorizationInfo domain) {
        AuthorizationInfoJpaEntity entity = new AuthorizationInfoJpaEntity();
        entity.setAuthorizationInfoId(domain.authorizationInfoId());
        entity.setProcessId(domain.processId());
        entity.setAuthorizationRequired(domain.authorizationRequired());
        entity.setAuthorizationState(domain.authorizationState().name());
        entity.setPendingReason(domain.pendingReason());
        entity.setAuthorizedAt(domain.authorizedAt());
        entity.setAuthorizationNote(domain.authorizationNote());
        entity.setLastAuthorizationUpdateAt(domain.lastAuthorizationUpdateAt());
        return entity;
    }

    public AuthorizationInfo toDomain(AuthorizationInfoJpaEntity entity) {
        return new AuthorizationInfo(
                entity.getAuthorizationInfoId(),
                entity.getProcessId(),
                entity.isAuthorizationRequired(),
                AuthorizationState.valueOf(entity.getAuthorizationState()),
                entity.getPendingReason(),
                entity.getAuthorizedAt(),
                entity.getAuthorizationNote(),
                entity.getLastAuthorizationUpdateAt()
        );
    }
}
