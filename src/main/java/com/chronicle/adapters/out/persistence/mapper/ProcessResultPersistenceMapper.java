package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.ProcessResultJpaEntity;
import com.chronicle.domain.model.Coverage;
import com.chronicle.domain.model.ExcludedDocument;
import com.chronicle.domain.model.ProcessResult;
import com.chronicle.domain.model.ResultKind;
import com.chronicle.domain.model.WordFrequency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProcessResultPersistenceMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<ExcludedDocument>> EXCLUDED_DOCUMENTS = new TypeReference<>() {
    };
    private static final TypeReference<List<WordFrequency>> WORD_FREQUENCIES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> DOCUMENT_SUMMARIES = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ProcessResultPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProcessResultJpaEntity toEntity(ProcessResult domain) {
        ProcessResultJpaEntity entity = new ProcessResultJpaEntity();
        entity.setProcessResultId(domain.processResultId());
        entity.setProcessId(domain.processId());
        entity.setResultKind(domain.resultKind().name());
        entity.setComputedAt(domain.computedAt());
        entity.setIncludedDocumentsJson(write(domain.includedDocuments()));
        entity.setExcludedDocumentsJson(write(domain.excludedDocuments()));
        entity.setTotalWords(domain.totalWords());
        entity.setTotalLines(domain.totalLines());
        entity.setTotalCharacters(domain.totalCharacters());
        entity.setMostFrequentWordsJson(write(domain.mostFrequentWords()));
        entity.setDocumentSummariesJson(write(domain.documentSummaries()));
        entity.setGlobalSummary(domain.globalSummary());
        entity.setPlannedFiles(domain.coverage().plannedFiles());
        entity.setIncludedFiles(domain.coverage().includedFiles());
        entity.setExcludedFiles(domain.coverage().excludedFiles());
        entity.setCoveragePercentage(domain.coverage().percentage());
        entity.setFinal(domain.isFinal());
        return entity;
    }

    public ProcessResult toDomain(ProcessResultJpaEntity entity) {
        return new ProcessResult(
                entity.getProcessResultId(),
                entity.getProcessId(),
                ResultKind.valueOf(entity.getResultKind()),
                entity.getComputedAt(),
                readList(entity.getIncludedDocumentsJson(), STRING_LIST),
                readList(entity.getExcludedDocumentsJson(), EXCLUDED_DOCUMENTS),
                entity.getTotalWords(),
                entity.getTotalLines(),
                entity.getTotalCharacters(),
                readList(entity.getMostFrequentWordsJson(), WORD_FREQUENCIES),
                readMap(entity.getDocumentSummariesJson(), DOCUMENT_SUMMARIES),
                entity.getGlobalSummary(),
                new Coverage(
                        entity.getPlannedFiles(),
                        entity.getIncludedFiles(),
                        entity.getExcludedFiles(),
                        entity.getCoveragePercentage()
                ),
                entity.isFinal()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize process result payload", exception);
        }
    }

    private <T> T readList(String json, TypeReference<T> typeReference) {
        try {
            return json == null || json.isBlank()
                    ? objectMapper.readValue("[]", typeReference)
                    : objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize process result payload", exception);
        }
    }

    private Map<String, String> readMap(String json, TypeReference<Map<String, String>> typeReference) {
        try {
            return json == null || json.isBlank()
                    ? Map.of()
                    : objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize process result payload", exception);
        }
    }
}
