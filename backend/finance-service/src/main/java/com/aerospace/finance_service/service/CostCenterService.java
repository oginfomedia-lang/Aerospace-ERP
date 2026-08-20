package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.CostCenterDTO;

import java.util.List;

public interface CostCenterService {
    CostCenterDTO create(CostCenterDTO dto);
    CostCenterDTO getById(Long id);
    List<CostCenterDTO> getAll();
    CostCenterDTO update(Long id, CostCenterDTO dto);
    void deactivate(Long id);
}
