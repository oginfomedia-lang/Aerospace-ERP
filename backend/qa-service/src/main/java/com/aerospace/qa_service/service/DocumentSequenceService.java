package com.aerospace.qa_service.service;

import java.util.List;

import com.aerospace.qa_service.entity.DocumentSequence;

public interface DocumentSequenceService {
    String generate(String docType);

    List<DocumentSequence> getAll();

    DocumentSequence updateConfig(String docType, String prefix, String suffix, Integer padding);
}
