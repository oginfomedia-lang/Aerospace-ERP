package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.entity.DocumentSequence;
import com.aerospace.procurement_service.repository.DocumentSequenceRepository;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

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

        return String.format("%s/%s/%05d", docType, yearKey, number);
    }
}
