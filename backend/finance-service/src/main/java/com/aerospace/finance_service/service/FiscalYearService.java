package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.FiscalYearDTO;

import java.util.List;

public interface FiscalYearService {
    FiscalYearDTO create(FiscalYearDTO dto);
    List<FiscalYearDTO> getAll();
}
