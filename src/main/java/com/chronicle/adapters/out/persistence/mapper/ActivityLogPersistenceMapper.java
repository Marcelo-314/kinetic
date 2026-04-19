package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ActivityLogJpaEntity;
import com.chronicle.domain.model.ActivityLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActivityLogPersistenceMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActivityLogJpaEntity toEntity(ActivityLogEntry domain) {
        ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
        entity.setActivityId(domain.activityId());
        entity.setProcessId(domain.processId());
        entity.setTimestamp(domain.timestamp());
        entity.setEventType(domain.eventType());
        entity.setEventStage(domain.eventStage());
        entity.setMessage(domain.message());
        entity.setMetadataJson(write(domain.metadata()));
        entity.setCorrelationId(domain.correlationId());
        return entity;
    }

    public ActivityLogEntry toDomain(ActivityLogJpaEntity entity) {
        return new ActivityLogEntry(
                entity.getActivityId(),
                entity.getProcessId(),
                entity.getTimestamp(),
                entity.getEventType(),
                entity.getEventStage(),
                entity.getMessage(),
                read(entity.getMetadataJson()),
                entity.getCorrelationId()
        );
    }

    private String write(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize activity metadata", e);
        }
    }

    private Map<String, Object> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize activity metadata", e);
        }
    }
}
