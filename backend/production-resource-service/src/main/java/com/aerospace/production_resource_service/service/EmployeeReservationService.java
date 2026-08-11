package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.EmployeeReservationRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeReservationResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface EmployeeReservationService {
    EmployeeReservationResponseDTO createReservation(EmployeeReservationRequestDTO dto);
    EmployeeReservationResponseDTO updateReservation(Long id, EmployeeReservationRequestDTO dto);
    EmployeeReservationResponseDTO getById(Long id);
    List<EmployeeReservationResponseDTO> listReservations(Long employeeId, LocalDateTime from, LocalDateTime to,
                                                            Long productionOrderId, String status);
    void cancelReservation(Long id);

    // Same purpose as MachineReservationService.checkActiveConflict — see there.
    EmployeeReservationResponseDTO checkActiveConflict(Long employeeId, Long excludeProductionOrderId);
}
