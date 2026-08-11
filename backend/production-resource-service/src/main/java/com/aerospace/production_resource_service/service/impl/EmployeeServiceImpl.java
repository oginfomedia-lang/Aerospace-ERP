package com.aerospace.production_resource_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.production_resource_service.dto.EmployeeRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeResponseDTO;
import com.aerospace.production_resource_service.dto.EmployeeSkillCertificationRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeSkillCertificationResponseDTO;
import com.aerospace.production_resource_service.dto.EmployeeSkillSummaryDTO;
import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeOccupancy;
import com.aerospace.production_resource_service.entity.EmployeeSkillCertification;
import com.aerospace.production_resource_service.entity.EmployeeStatus;
import com.aerospace.production_resource_service.entity.LeaveStatus;
import com.aerospace.production_resource_service.entity.Shift;
import com.aerospace.production_resource_service.entity.Skill;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.EmployeeLeaveRepository;
import com.aerospace.production_resource_service.repository.EmployeeRepository;
import com.aerospace.production_resource_service.repository.EmployeeSkillCertificationRepository;
import com.aerospace.production_resource_service.repository.ShiftRepository;
import com.aerospace.production_resource_service.repository.SkillRepository;
import com.aerospace.production_resource_service.service.EmployeeService;
import com.aerospace.production_resource_service.util.ShiftWindowUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillCertificationRepository certificationRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
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
                .shift(resolveShift(dto.getShiftId()))
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
        employee.setShift(resolveShift(dto.getShiftId()));
        if (dto.getStatus() != null) {
            employee.setStatus(EmployeeStatus.valueOf(dto.getStatus()));
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated — employeeCode: {}", saved.getEmployeeCode());
        return toDTO(saved);
    }

    // Null shiftId is a valid "no shift assigned" choice, not an error — only a
    // non-null id that fails to resolve is a real problem.
    private Shift resolveShift(Long shiftId) {
        if (shiftId == null) return null;
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with ID: " + shiftId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> listEmployees(String status, Long skillId) {
        List<Employee> employees = status != null
                ? employeeRepository.findByStatus(EmployeeStatus.valueOf(status))
                : employeeRepository.findAll();

        if (skillId != null) {
            Set<Long> eligibleIds = Set.copyOf(certificationRepository.findActiveEmployeeIdsBySkill(skillId));
            employees = employees.stream().filter(e -> eligibleIds.contains(e.getId())).toList();
        }
        return employees.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO occupyEmployee(Long id, Long stageLogId) {
        Employee employee = findOrThrow(id);
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is not ACTIVE (current: " + employee.getStatus() + ")");
        }
        if (employee.getOccupancy() == EmployeeOccupancy.IN_OPERATION) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is currently assigned to another operation");
        }
        if (employee.getShift() != null && !ShiftWindowUtil.isWithin(employee.getShift(), LocalDateTime.now())) {
            Shift shift = employee.getShift();
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is outside their shift window ("
                            + shift.getName() + ", " + shift.getStartTime() + "-" + shift.getEndTime()
                            + ", " + shift.getDaysOfWeek() + ")");
        }
        if (employeeLeaveRepository.existsByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee.getId(), LeaveStatus.APPROVED, LocalDate.now(), LocalDate.now())) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is on approved leave today");
        }

        employee.setOccupancy(EmployeeOccupancy.IN_OPERATION);
        employee.setCurrentStageLogId(stageLogId);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee occupied — employeeCode: {}, stageLogId: {}", saved.getEmployeeCode(), stageLogId);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeResponseDTO releaseEmployee(Long id) {
        Employee employee = findOrThrow(id);
        employee.setOccupancy(EmployeeOccupancy.FREE);
        employee.setCurrentStageLogId(null);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee released — employeeCode: {}", saved.getEmployeeCode());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeSkillCertificationResponseDTO recordSkillCertification(Long employeeId, EmployeeSkillCertificationRequestDTO dto) {
        Employee employee = findOrThrow(employeeId);
        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + dto.getSkillId()));
        if (skill.isCertificationRequired() && (dto.getCertificateNumber() == null || dto.getCertificateNumber().isBlank())) {
            throw new IllegalArgumentException(
                    "Skill '" + skill.getName() + "' requires a certificate number to be recorded");
        }

        // dto.getStatus() lets a supervisor explicitly record a REVOKED row (e.g. "this
        // certification is no longer valid after a safety incident") — without this, the
        // entity's @Builder.Default ACTIVE was the only value ever stored, even though
        // effectiveStatus() already knew how to report REVOKED if one existed. Omitting
        // status entirely (the normal case) still defaults to ACTIVE, unchanged.
        com.aerospace.production_resource_service.entity.CertificationStatus status =
                dto.getStatus() != null && !dto.getStatus().isBlank()
                        ? com.aerospace.production_resource_service.entity.CertificationStatus.valueOf(dto.getStatus())
                        : com.aerospace.production_resource_service.entity.CertificationStatus.ACTIVE;

        EmployeeSkillCertification cert = EmployeeSkillCertification.builder()
                .employee(employee)
                .skill(skill)
                .level(dto.getLevel())
                .experienceHours(dto.getExperienceHours())
                .certificateNumber(dto.getCertificateNumber())
                .issuedDate(dto.getIssuedDate())
                .expiryDate(dto.getExpiryDate())
                .evaluatedBy(dto.getEvaluatedBy())
                .evaluationDate(dto.getEvaluationDate())
                .nextEvaluationDate(dto.getNextEvaluationDate())
                .remarks(dto.getRemarks())
                .status(status)
                .build();

        EmployeeSkillCertification saved = certificationRepository.save(cert);
        log.info("Skill certification recorded — employeeCode: {}, skill: {}, level: {}",
                employee.getEmployeeCode(), skill.getName(), dto.getLevel());
        return toCertDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSkillCertificationResponseDTO> getSkillCertificationHistory(Long employeeId) {
        findOrThrow(employeeId);
        return certificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream().map(this::toCertDTO).toList();
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    private EmployeeResponseDTO toDTO(Employee e) {
        return EmployeeResponseDTO.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .name(e.getName())
                .designation(e.getDesignation())
                .department(e.getDepartment())
                .status(e.getStatus().name())
                .skills(currentSkillSummaries(e.getId()))
                .shiftId(e.getShift() != null ? e.getShift().getId() : null)
                .shiftName(e.getShift() != null ? e.getShift().getName() : null)
                .occupancy(e.getOccupancy().name())
                .currentStageLogId(e.getCurrentStageLogId())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .build();
    }

    // Latest EmployeeSkillCertification row per skill, newest-first input means the
    // first row seen per skillId IS the current one — mirrors how Machine derives
    // lastMaintenanceAt/nextMaintenanceDueAt from its latest MachineMaintenanceRecord.
    private List<EmployeeSkillSummaryDTO> currentSkillSummaries(Long employeeId) {
        List<EmployeeSkillCertification> history = certificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        Map<Long, EmployeeSkillCertification> latestPerSkill = new LinkedHashMap<>();
        for (EmployeeSkillCertification c : history) {
            latestPerSkill.putIfAbsent(c.getSkill().getId(), c);
        }
        return latestPerSkill.values().stream()
                .map(c -> EmployeeSkillSummaryDTO.builder()
                        .skillId(c.getSkill().getId())
                        .skillName(c.getSkill().getName())
                        .level(c.getLevel())
                        .expiryDate(c.getExpiryDate())
                        .status(effectiveStatus(c).name())
                        .build())
                .collect(Collectors.toList());
    }

    // A cert can be recorded ACTIVE but have since aged past its own expiryDate —
    // report EXPIRED in that case even though the stored status column still says
    // ACTIVE, rather than requiring a separate batch job to flip it.
    private com.aerospace.production_resource_service.entity.CertificationStatus effectiveStatus(EmployeeSkillCertification c) {
        if (c.getStatus() == com.aerospace.production_resource_service.entity.CertificationStatus.REVOKED) {
            return com.aerospace.production_resource_service.entity.CertificationStatus.REVOKED;
        }
        if (c.getExpiryDate() != null && c.getExpiryDate().isBefore(LocalDate.now())) {
            return com.aerospace.production_resource_service.entity.CertificationStatus.EXPIRED;
        }
        return c.getStatus();
    }

    private EmployeeSkillCertificationResponseDTO toCertDTO(EmployeeSkillCertification c) {
        return EmployeeSkillCertificationResponseDTO.builder()
                .id(c.getId())
                .employeeId(c.getEmployee().getId())
                .skillId(c.getSkill().getId())
                .skillName(c.getSkill().getName())
                .level(c.getLevel())
                .experienceHours(c.getExperienceHours())
                .certificateNumber(c.getCertificateNumber())
                .issuedDate(c.getIssuedDate())
                .expiryDate(c.getExpiryDate())
                .evaluatedBy(c.getEvaluatedBy())
                .evaluationDate(c.getEvaluationDate())
                .nextEvaluationDate(c.getNextEvaluationDate())
                .status(effectiveStatus(c).name())
                .remarks(c.getRemarks())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
