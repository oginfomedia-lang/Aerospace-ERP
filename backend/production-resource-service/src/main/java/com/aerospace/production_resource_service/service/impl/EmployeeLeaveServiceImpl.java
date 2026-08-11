package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.EmployeeLeaveRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeLeaveResponseDTO;
import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeLeave;
import com.aerospace.production_resource_service.entity.LeaveStatus;
import com.aerospace.production_resource_service.entity.LeaveType;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.EmployeeLeaveRepository;
import com.aerospace.production_resource_service.repository.EmployeeRepository;
import com.aerospace.production_resource_service.service.EmployeeLeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeLeaveServiceImpl implements EmployeeLeaveService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;

    @Override
    @Transactional
    public EmployeeLeaveResponseDTO requestLeave(Long employeeId, EmployeeLeaveRequestDTO dto) {
        Employee employee = findEmployeeOrThrow(employeeId);
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        EmployeeLeave leave = EmployeeLeave.builder()
                .employee(employee)
                .type(LeaveType.valueOf(dto.getType()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(LeaveStatus.PENDING)
                .build();
        EmployeeLeave saved = employeeLeaveRepository.save(leave);
        log.info("Leave requested — employeeCode: {}, {} to {}", employee.getEmployeeCode(),
                saved.getStartDate(), saved.getEndDate());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeLeaveResponseDTO approveLeave(Long employeeId, Long leaveId, String decidedBy) {
        return decideLeave(employeeId, leaveId, decidedBy, LeaveStatus.APPROVED);
    }

    @Override
    @Transactional
    public EmployeeLeaveResponseDTO rejectLeave(Long employeeId, Long leaveId, String decidedBy) {
        return decideLeave(employeeId, leaveId, decidedBy, LeaveStatus.REJECTED);
    }

    private EmployeeLeaveResponseDTO decideLeave(Long employeeId, Long leaveId, String decidedBy, LeaveStatus decision) {
        findEmployeeOrThrow(employeeId); // validate employee exists
        EmployeeLeave leave = employeeLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));
        if (!leave.getEmployee().getId().equals(employeeId)) {
            throw new IllegalStateException("Leave request " + leaveId + " does not belong to employee " + employeeId);
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request is already " + leave.getStatus() + " — cannot decide again");
        }
        leave.setStatus(decision);
        leave.setDecidedBy(decidedBy);
        leave.setDecidedAt(LocalDateTime.now());
        EmployeeLeave saved = employeeLeaveRepository.save(leave);
        log.info("Leave request {} — employeeCode: {}, decidedBy: {}", decision, leave.getEmployee().getEmployeeCode(), decidedBy);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeLeaveResponseDTO> getLeaveHistory(Long employeeId) {
        findEmployeeOrThrow(employeeId);
        return employeeLeaveRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream().map(this::toDTO).toList();
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + id));
    }

    private EmployeeLeaveResponseDTO toDTO(EmployeeLeave l) {
        return EmployeeLeaveResponseDTO.builder()
                .id(l.getId())
                .employeeId(l.getEmployee().getId())
                .employeeCode(l.getEmployee().getEmployeeCode())
                .type(l.getType().name())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .reason(l.getReason())
                .status(l.getStatus().name())
                .decidedBy(l.getDecidedBy())
                .decidedAt(l.getDecidedAt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
