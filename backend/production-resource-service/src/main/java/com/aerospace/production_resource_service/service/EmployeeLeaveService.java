package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.EmployeeLeaveRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeLeaveResponseDTO;

import java.util.List;

public interface EmployeeLeaveService {
    EmployeeLeaveResponseDTO requestLeave(Long employeeId, EmployeeLeaveRequestDTO dto);
    EmployeeLeaveResponseDTO approveLeave(Long employeeId, Long leaveId, String decidedBy);
    EmployeeLeaveResponseDTO rejectLeave(Long employeeId, Long leaveId, String decidedBy);
    List<EmployeeLeaveResponseDTO> getLeaveHistory(Long employeeId);
}
