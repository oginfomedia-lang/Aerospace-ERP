package com.aerospace.production_resource_service.service;

import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;
import com.aerospace.production_resource_service.dto.EmployeeSkillCertificationRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeSkillCertificationResponseDTO;

import java.util.List;

public interface EmployeeService {
    EmployeeResponseDTO registerEmployee(EmployeeRequestDTO dto);
    EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto);
    List<EmployeeResponseDTO> listEmployees(String status, Long skillId);
    EmployeeResponseDTO getEmployeeById(Long id);
    EmployeeResponseDTO occupyEmployee(Long id, Long stageLogId);
    EmployeeResponseDTO releaseEmployee(Long id);
    EmployeeSkillCertificationResponseDTO recordSkillCertification(Long employeeId, EmployeeSkillCertificationRequestDTO dto);
    List<EmployeeSkillCertificationResponseDTO> getSkillCertificationHistory(Long employeeId);
}
