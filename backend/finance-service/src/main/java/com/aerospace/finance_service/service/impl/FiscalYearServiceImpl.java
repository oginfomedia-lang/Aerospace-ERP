package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.FiscalYearDTO;
import com.aerospace.finance_service.entity.FiscalYear;
import com.aerospace.finance_service.repository.FiscalYearRepository;
import com.aerospace.finance_service.service.FiscalYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalYearServiceImpl implements FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;

    @Override
    @Transactional
    public FiscalYearDTO create(FiscalYearDTO dto) {
        if (fiscalYearRepository.existsByFiscalYearCode(dto.getFiscalYearCode())) {
            throw new IllegalArgumentException("Fiscal year code already exists: " + dto.getFiscalYearCode());
        }
        FiscalYear entity = FiscalYear.builder()
                .fiscalYearCode(dto.getFiscalYearCode())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        return toDTO(fiscalYearRepository.save(entity));
    }

    @Override
    public List<FiscalYearDTO> getAll() {
        return fiscalYearRepository.findAll().stream().map(this::toDTO).toList();
    }

    private FiscalYearDTO toDTO(FiscalYear e) {
        return FiscalYearDTO.builder()
                .id(e.getId()).fiscalYearCode(e.getFiscalYearCode())
                .startDate(e.getStartDate()).endDate(e.getEndDate()).status(e.getStatus())
                .build();
    }
}
