package com.aerospace.qa_service.service.impl;

import com.aerospace.qa_service.entity.DocumentSequence;
import com.aerospace.qa_service.repository.DocumentSequenceRepository;
import com.aerospace.qa_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSequenceServiceImpl implements DocumentSequenceService {

    private final DocumentSequenceRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(String docType) {
        String yearKey = String.valueOf(Year.now().getValue());

        DocumentSequence seq = repository.lockByDocTypeAndYearKey(docType, yearKey)
                .orElseGet(() -> DocumentSequence.builder()
                        .docType(docType).yearKey(yearKey).nextNumber(1L).build());

        long number = seq.getNextNumber();
        seq.setNextNumber(number + 1);
        repository.save(seq);

        String effectivePrefix = seq.getPrefix() != null ? seq.getPrefix() : docType;
        String numberPart = String.format("%0" + seq.getPadding() + "d", number);
        return seq.getSuffix() != null ? String.format("%s/%s/%s/%s", effectivePrefix, yearKey, numberPart, seq.getSuffix()) : String.format("%s/%s/%s", effectivePrefix, yearKey, numberPart);
    }

    @Override
    public List<DocumentSequence> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public DocumentSequence updateConfig(String docType, String prefix, String suffix, Integer padding) {
        String yearKey = String.valueOf(Year.now().getValue());
        DocumentSequence seq = repository.lockByDocTypeAndYearKey(docType, yearKey)
                .orElseGet(() -> DocumentSequence.builder()
                        .docType(docType).yearKey(yearKey).nextNumber(1L).padding(5).build());
        if (prefix != null) seq.setPrefix(prefix);
        if (suffix != null) seq.setSuffix(suffix);
        if (padding != null) seq.setPadding(padding);
        return repository.save(seq);
    }
}

