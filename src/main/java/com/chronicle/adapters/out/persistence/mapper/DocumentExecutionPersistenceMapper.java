package com.chronicle.adapters.out.persistence.mapper;

import com.chronicle.adapters.out.persistence.entity.DocumentExecutionJpaEntity;
import com.chronicle.domain.model.DocumentExecution;
import com.chronicle.domain.model.DocumentStatus;
import com.chronicle.domain.model.SummaryPolicy;
import com.chronicle.domain.model.WordFrequency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentExecutionPersistenceMapper {

    private static final TypeReference<List<WordFrequency>> WORD_FREQUENCIES = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public DocumentExecutionPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentExecutionJpaEntity toEntity(DocumentExecution domain) {
        DocumentExecutionJpaEntity entity = new DocumentExecutionJpaEntity();
        entity.setDocumentExecutionId(domain.documentExecutionId());
        entity.setProcessId(domain.processId());
        entity.setDocumentName(domain.documentName());
        entity.setDocumentPath(domain.documentPath());
        entity.setDocumentStatus(domain.documentStatus().name());
        entity.setBatchIndex(domain.batchIndex());
        entity.setStartedAt(domain.startedAt());
        entity.setFinishedAt(domain.finishedAt());
        entity.setWordCount(domain.wordCount());
        entity.setLineCount(domain.lineCount());
        entity.setCharacterCount(domain.characterCount());
        entity.setMostFrequentWordsJson(writeWordFrequencies(domain.mostFrequentWords()));
        entity.setSummary(domain.summary());
        entity.setSummaryMethod(domain.summaryMethod() == null ? null : domain.summaryMethod().name());
        entity.setErrorCode(domain.errorCode());
        entity.setErrorMessage(domain.errorMessage());
        return entity;
    }

    public DocumentExecution toDomain(DocumentExecutionJpaEntity entity) {
        return new DocumentExecution(
                entity.getDocumentExecutionId(),
                entity.getProcessId(),
                entity.getDocumentName(),
                entity.getDocumentPath(),
                DocumentStatus.valueOf(entity.getDocumentStatus()),
                entity.getBatchIndex(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getWordCount(),
                entity.getLineCount(),
                entity.getCharacterCount(),
                readWordFrequencies(entity.getMostFrequentWordsJson()),
                entity.getSummary(),
                entity.getSummaryMethod() == null ? null : SummaryPolicy.valueOf(entity.getSummaryMethod()),
                entity.getErrorCode(),
                entity.getErrorMessage()
        );
    }

    private String writeWordFrequencies(List<WordFrequency> words) {
        try {
            return objectMapper.writeValueAsString(words);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize word frequencies", exception);
        }
    }

    private List<WordFrequency> readWordFrequencies(String json) {
        try {
            return json == null || json.isBlank()
                    ? List.of()
                    : objectMapper.readValue(json, WORD_FREQUENCIES);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize word frequencies", exception);
        }
    }
}
