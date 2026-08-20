package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.ProfitCenterDTO;

import java.util.List;

public interface ProfitCenterService {
    ProfitCenterDTO create(ProfitCenterDTO dto);
    ProfitCenterDTO getById(Long id);
    List<ProfitCenterDTO> getAll();
    ProfitCenterDTO update(Long id, ProfitCenterDTO dto);
    void deactivate(Long id);
}
