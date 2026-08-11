package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.ShiftRequestDTO;
import com.aerospace.production_resource_service.dto.ShiftResponseDTO;

import java.util.List;

public interface ShiftService {
    ShiftResponseDTO createShift(ShiftRequestDTO dto);
    List<ShiftResponseDTO> listShifts();
    ShiftResponseDTO getShiftById(Long id);
}
