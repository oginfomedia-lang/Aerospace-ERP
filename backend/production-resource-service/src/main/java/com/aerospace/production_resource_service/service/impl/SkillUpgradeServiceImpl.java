package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.EmployeeSkillCertificationRequestDTO;
import com.aerospace.production_resource_service.dto.SkillUpgradeRequestCreateDTO;
import com.aerospace.production_resource_service.dto.SkillUpgradeResponseDTO;
import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeSkillCertification;
import com.aerospace.production_resource_service.entity.Skill;
import com.aerospace.production_resource_service.entity.SkillUpgradeRequest;
import com.aerospace.production_resource_service.entity.SkillUpgradeStatus;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.EmployeeRepository;
import com.aerospace.production_resource_service.repository.EmployeeSkillCertificationRepository;
import com.aerospace.production_resource_service.repository.SkillRepository;
import com.aerospace.production_resource_service.repository.SkillUpgradeRequestRepository;
import com.aerospace.production_resource_service.service.SkillUpgradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillUpgradeServiceImpl implements SkillUpgradeService {

    private final SkillUpgradeRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillCertificationRepository certificationRepository;
    // Concrete impl, not the interface — recordSkillCertificationInternal is a
    // package-private bypass of the direct-upgrade guard, only meant for this one caller.
    private final EmployeeServiceImpl employeeServiceImpl;

    @Override
    @Transactional
    public SkillUpgradeResponseDTO requestUpgrade(Long employeeId, SkillUpgradeRequestCreateDTO dto, String requestedBy) {
        Employee employee = findEmployeeOrThrow(employeeId);
        Skill skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with ID: " + dto.getSkillId()));

        int fromLevel = currentLevel(employeeId, dto.getSkillId());
        if (dto.getToLevel() <= fromLevel) {
            throw new IllegalArgumentException(
                    "New level (" + dto.getToLevel() + ") must be higher than the current level (" + fromLevel + ")");
        }

        SkillUpgradeRequest request = SkillUpgradeRequest.builder()
                .employee(employee)
                .skill(skill)
                .fromLevel(fromLevel)
                .toLevel(dto.getToLevel())
                .reason(dto.getReason())
                .requestedBy(requestedBy)
                .status(SkillUpgradeStatus.PENDING)
                .build();
        SkillUpgradeRequest saved = requestRepository.save(request);
        log.info("Skill upgrade requested — employeeCode: {}, skill: {}, {} -> {}",
                employee.getEmployeeCode(), skill.getName(), fromLevel, dto.getToLevel());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillUpgradeResponseDTO> getHistory(Long employeeId) {
        findEmployeeOrThrow(employeeId);
        return requestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillUpgradeResponseDTO> getPending() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(SkillUpgradeStatus.PENDING).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public SkillUpgradeResponseDTO approve(Long employeeId, Long requestId, String decidedBy) {
        SkillUpgradeRequest request = findRequestOrThrow(employeeId, requestId);
        if (request.getStatus() != SkillUpgradeStatus.PENDING) {
            throw new IllegalStateException("Request already decided (current status: " + request.getStatus() + ")");
        }
        request.setStatus(SkillUpgradeStatus.APPROVED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(LocalDateTime.now());
        SkillUpgradeRequest saved = requestRepository.save(request);

        EmployeeSkillCertificationRequestDTO certDto = new EmployeeSkillCertificationRequestDTO();
        certDto.setSkillId(request.getSkill().getId());
        certDto.setLevel(request.getToLevel());
        certDto.setEvaluatedBy(decidedBy);
        certDto.setEvaluationDate(LocalDate.now());
        certDto.setRemarks("Approved via skill upgrade request #" + requestId
                + (request.getReason() != null && !request.getReason().isBlank() ? " — " + request.getReason() : ""));
        certDto.setStatus("ACTIVE");
        employeeServiceImpl.recordSkillCertificationInternal(employeeId, certDto);

        log.info("Skill upgrade approved — requestId: {}, employeeCode: {}, skill: {}, newLevel: {}",
                requestId, saved.getEmployee().getEmployeeCode(), saved.getSkill().getName(), saved.getToLevel());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public SkillUpgradeResponseDTO reject(Long employeeId, Long requestId, String decidedBy) {
        SkillUpgradeRequest request = findRequestOrThrow(employeeId, requestId);
        if (request.getStatus() != SkillUpgradeStatus.PENDING) {
            throw new IllegalStateException("Request already decided (current status: " + request.getStatus() + ")");
        }
        request.setStatus(SkillUpgradeStatus.REJECTED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(LocalDateTime.now());
        SkillUpgradeRequest saved = requestRepository.save(request);
        log.info("Skill upgrade rejected — requestId: {}, employeeCode: {}", requestId, saved.getEmployee().getEmployeeCode());
        return toDTO(saved);
    }

    private int currentLevel(Long employeeId, Long skillId) {
        return certificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .filter(c -> c.getSkill().getId().equals(skillId))
                .findFirst()
                .map(EmployeeSkillCertification::getLevel)
                .orElse(0);
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    private SkillUpgradeRequest findRequestOrThrow(Long employeeId, Long requestId) {
        SkillUpgradeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill upgrade request not found: " + requestId));
        if (!request.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException("Skill upgrade request " + requestId + " does not belong to employee " + employeeId);
        }
        return request;
    }

    private SkillUpgradeResponseDTO toDTO(SkillUpgradeRequest r) {
        return SkillUpgradeResponseDTO.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeName(r.getEmployee().getName())
                .skillId(r.getSkill().getId())
                .skillName(r.getSkill().getName())
                .fromLevel(r.getFromLevel())
                .toLevel(r.getToLevel())
                .status(r.getStatus().name())
                .reason(r.getReason())
                .requestedBy(r.getRequestedBy())
                .createdAt(r.getCreatedAt())
                .decidedBy(r.getDecidedBy())
                .decidedAt(r.getDecidedAt())
                .build();
    }
}
