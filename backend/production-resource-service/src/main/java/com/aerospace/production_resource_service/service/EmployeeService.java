package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;

import java.util.List;

public interface EmployeeService {
    EmployeeResponseDTO registerEmployee(EmployeeRequestDTO dto);
    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto);
    List<EmployeeResponseDTO> listEmployees(String status, Long skillId);
    EmployeeResponseDTO getEmployeeById(Long id);
}
