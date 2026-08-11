package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.MachineReservationRequestDTO;
import com.aerospace.production_resource_service.dto.MachineReservationResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface MachineReservationService {
    MachineReservationResponseDTO createReservation(MachineReservationRequestDTO dto);
    MachineReservationResponseDTO updateReservation(Long id, MachineReservationRequestDTO dto);
    MachineReservationResponseDTO getById(Long id);
    List<MachineReservationResponseDTO> listReservations(Long machineId, LocalDateTime from, LocalDateTime to,
                                                           Long productionOrderId, String status);
    void cancelReservation(Long id);

    // Null return = no conflict. Used by production-service's startStage to check
    // whether starting this machine right now would clash with someone else's
    // advance booking — see MachineReservationServiceImpl for the full explanation.
    MachineReservationResponseDTO checkActiveConflict(Long machineId, Long excludeProductionOrderId);
}
