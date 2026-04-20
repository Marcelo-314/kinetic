package com.chronicle.domain.model;

public record ExcludedDocument(
        String documentName,
        String reasonCode
) {

    public ExcludedDocument {
        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("documentName must not be blank");
        }
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("reasonCode must not be blank");
        }
    }
}
