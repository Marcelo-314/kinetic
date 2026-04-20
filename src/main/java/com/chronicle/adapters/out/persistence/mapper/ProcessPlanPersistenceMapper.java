package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ProcessPlanJpaEntity;
import com.chronicle.domain.model.FailurePolicy;
import com.chronicle.domain.model.ProcessPlan;
import com.chronicle.domain.model.SelectionMode;
import com.chronicle.domain.model.SummaryPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessPlanPersistenceMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessPlanJpaEntity toEntity(ProcessPlan domain) {
        ProcessPlanJpaEntity entity = new ProcessPlanJpaEntity();
        entity.setPlanId(domain.planId());
        entity.setProcessId(domain.processId());
        entity.setSourceFolder(domain.sourceFolder());
        entity.setSelectionMode(domain.selectionMode().name());
        entity.setSelectedFilesJson(write(domain.selectedFiles()));
        entity.setTotalPlannedFiles(domain.totalPlannedFiles());
        entity.setBatchSize(domain.batchSize());
        entity.setSummaryPolicy(domain.summaryPolicy().name());
        entity.setFailurePolicy(domain.failurePolicy().name());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }

    public ProcessPlan toDomain(ProcessPlanJpaEntity entity) {
        return new ProcessPlan(
                entity.getPlanId(),
                entity.getProcessId(),
                entity.getSourceFolder(),
                SelectionMode.valueOf(entity.getSelectionMode()),
                readList(entity.getSelectedFilesJson()),
                entity.getTotalPlannedFiles(),
                entity.getBatchSize(),
                SummaryPolicy.valueOf(entity.getSummaryPolicy()),
                FailurePolicy.valueOf(entity.getFailurePolicy()),
                entity.getCreatedAt()
        );
    }

    private String write(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize selected files", e);
        }
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize selected files", e);
        }
    }
}
