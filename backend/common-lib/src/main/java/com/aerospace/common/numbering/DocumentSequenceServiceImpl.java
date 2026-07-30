package com.aerospace.common.numbering;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSequenceServiceImpl implements DocumentSequenceService {

    private static final String DEFAULT_SCOPE = "DEFAULT";

    private final DocumentSequenceRepository repository;

    @Override
    public String generate(String docType) {
        return generate(docType, DEFAULT_SCOPE, DEFAULT_SCOPE);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate(String docType, String companyCode, String plantCode) {
        String yearKey = String.valueOf(Year.now().getValue());

        DocumentSequence seq = repository.lockRow(docType, yearKey, companyCode, plantCode)
                .orElseGet(() -> DocumentSequence.builder()
                        .docType(docType).yearKey(yearKey)
                        .companyCode(companyCode).plantCode(plantCode)
                        .nextNumber(1L).padding(6).build());

        long number = seq.getNextNumber();
        seq.setNextNumber(number + 1);
        repository.save(seq);

        String effectivePrefix = seq.getPrefix() != null ? seq.getPrefix() : docType;
        String numberPart = String.format("%0" + seq.getPadding() + "d", number);
        // Standard format from chapter 7.9: PREFIX-YEAR-NUMBER[-SUFFIX]
        return seq.getSuffix() != null
                ? String.format("%s-%s-%s-%s", effectivePrefix, yearKey, numberPart, seq.getSuffix())
                : String.format("%s-%s-%s", effectivePrefix, yearKey, numberPart);
    }

    @Override
    public List<DocumentSequence> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public DocumentSequence updateConfig(String docType, String prefix, String suffix, Integer padding) {
        String yearKey = String.valueOf(Year.now().getValue());
        DocumentSequence seq = repository.lockRow(docType, yearKey, DEFAULT_SCOPE, DEFAULT_SCOPE)
                .orElseGet(() -> DocumentSequence.builder()
                        .docType(docType).yearKey(yearKey).nextNumber(1L).padding(6).build());
        if (prefix != null) seq.setPrefix(prefix);
        if (suffix != null) seq.setSuffix(suffix);
        if (padding != null) seq.setPadding(padding);
        return repository.save(seq);
    }
}