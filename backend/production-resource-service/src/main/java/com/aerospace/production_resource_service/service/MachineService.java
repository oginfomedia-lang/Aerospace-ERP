package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.*;
import java.util.List;

public interface MachineService {
    MachineResponseDTO registerMachine(MachineRequestDTO dto);
    List<MachineResponseDTO> listMachines(String status);
    MachineResponseDTO getMachineById(Long id);
    MaintenanceRecordResponseDTO recordMaintenance(Long machineId, MaintenanceRequestDTO dto);
    List<MaintenanceRecordResponseDTO> getMaintenanceHistory(Long machineId);
    List<MachineResponseDTO> getDueList(int withinDays);
}
