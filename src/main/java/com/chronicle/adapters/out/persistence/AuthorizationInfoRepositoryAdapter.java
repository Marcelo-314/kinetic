package com.chronicle.adapters.out.persistence;

import com.chronicle.adapters.out.persistence.mapper.AuthorizationInfoPersistenceMapper;
import com.chronicle.adapters.out.persistence.springdata.SpringDataAuthorizationInfoJpaRepository;
import com.chronicle.domain.model.AuthorizationInfo;
import com.chronicle.domain.port.AuthorizationInfoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AuthorizationInfoRepositoryAdapter implements AuthorizationInfoRepository {

    private final SpringDataAuthorizationInfoJpaRepository repository;
    private final AuthorizationInfoPersistenceMapper mapper;

    public AuthorizationInfoRepositoryAdapter(SpringDataAuthorizationInfoJpaRepository repository, AuthorizationInfoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuthorizationInfo save(AuthorizationInfo authorizationInfo) {
        return mapper.toDomain(repository.save(mapper.toEntity(authorizationInfo)));
    }

    @Override
    public Optional<AuthorizationInfo> findByProcessId(String processId) {
        return repository.findByProcessId(processId).map(mapper::toDomain);
    }
}
