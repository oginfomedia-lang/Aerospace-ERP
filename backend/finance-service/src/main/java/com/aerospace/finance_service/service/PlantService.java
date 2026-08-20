package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.PlantDTO;

import java.util.List;

public interface PlantService {
    PlantDTO create(PlantDTO dto);
    PlantDTO getById(Long id);
    List<PlantDTO> getAll();
    PlantDTO update(Long id, PlantDTO dto);
    void deactivate(Long id);
}
