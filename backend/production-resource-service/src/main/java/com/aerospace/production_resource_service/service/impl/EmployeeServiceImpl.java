package com.aerospace.production_resource_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;
import com.aerospace.production_resource_service.dto.SkillResponseDTO;
import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeStatus;
import com.aerospace.production_resource_service.entity.Skill;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.EmployeeRepository;
import com.aerospace.production_resource_service.repository.SkillRepository;
import com.aerospace.production_resource_service.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final DocumentSequenceService documentSequenceService;

    @Override
    @Transactional
    public EmployeeResponseDTO registerEmployee(EmployeeRequestDTO dto) {
        Employee employee = Employee.builder()
                .employeeCode(documentSequenceService.generate("EMP"))
                .name(dto.getName())
                .designation(dto.getDesignation())
                .department(dto.getDepartment())
                .status(EmployeeStatus.ACTIVE)
                .skills(resolveSkills(dto.getSkillIds()))
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Employee registered — employeeCode: {}, name: {}", saved.getEmployeeCode(), saved.getName());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO dto) {
        Employee employee = findOrThrow(id);
        employee.setName(dto.getName());
        employee.setDesignation(dto.getDesignation());
        employee.setDepartment(dto.getDepartment());
        if (dto.getStatus() != null) {
            employee.setStatus(EmployeeStatus.valueOf(dto.getStatus()));
        }
        employee.setSkills(resolveSkills(dto.getSkillIds()));

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated — employeeCode: {}", saved.getEmployeeCode());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> listEmployees(String status, Long skillId) {
        List<Employee> employees;
        EmployeeStatus statusEnum = status != null ? EmployeeStatus.valueOf(status) : null;
        if (statusEnum != null && skillId != null) {
            employees = employeeRepository.findByStatusAndSkillsId(statusEnum, skillId);
        } else if (statusEnum != null) {
            employees = employeeRepository.findByStatus(statusEnum);
        } else if (skillId != null) {
            employees = employeeRepository.findBySkillsId(skillId);
        } else {
            employees = employeeRepository.findAll();
        }
        return employees.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private Set<Skill> resolveSkills(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) return new HashSet<>();
        List<Skill> found = skillRepository.findAllById(skillIds);
        if (found.size() != new HashSet<>(skillIds).size()) {
            throw new ResourceNotFoundException("One or more skill IDs not found: " + skillIds);
        }
        return new HashSet<>(found);
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    private EmployeeResponseDTO toDTO(Employee e) {
        List<SkillResponseDTO> skillDTOs = e.getSkills().stream()
                .map(s -> SkillResponseDTO.builder().id(s.getId()).name(s.getName()).description(s.getDescription()).build())
                .toList();
        return EmployeeResponseDTO.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .name(e.getName())
                .designation(e.getDesignation())
                .department(e.getDepartment())
                .status(e.getStatus().name())
                .skills(skillDTOs)
                .createdAt(e.getCreatedAt())
                .build();
    }
}
