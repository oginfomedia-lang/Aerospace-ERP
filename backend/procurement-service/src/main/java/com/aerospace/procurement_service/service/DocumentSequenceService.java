package com.aerospace.procurement_service.service;

import java.util.List;

import com.aerospace.procurement_service.entity.DocumentSequence;

public interface DocumentSequenceService {
    // Generates the next number for the given document type, formatted as
    // "{DOC_TYPE}/{YEAR}/{00001}" (ARC-BR-006). Always system-generated.
    String generate(String docType);

    List<DocumentSequence> getAll();

    DocumentSequence updateConfig(String docType, String prefix, String suffix, Integer padding);
}
