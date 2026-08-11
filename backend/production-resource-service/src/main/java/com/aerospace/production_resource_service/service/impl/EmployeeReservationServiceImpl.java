package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.EmployeeReservationRequestDTO;
import com.aerospace.production_resource_service.dto.EmployeeReservationResponseDTO;
import com.aerospace.production_resource_service.entity.CertificationStatus;
import com.aerospace.production_resource_service.entity.Employee;
import com.aerospace.production_resource_service.entity.EmployeeReservation;
import com.aerospace.production_resource_service.entity.EmployeeSkillCertification;
import com.aerospace.production_resource_service.entity.EmployeeStatus;
import com.aerospace.production_resource_service.entity.LeaveStatus;
import com.aerospace.production_resource_service.entity.ReservationStatus;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.EmployeeLeaveRepository;
import com.aerospace.production_resource_service.repository.EmployeeReservationRepository;
import com.aerospace.production_resource_service.repository.EmployeeRepository;
import com.aerospace.production_resource_service.repository.EmployeeSkillCertificationRepository;
import com.aerospace.production_resource_service.service.EmployeeReservationService;
import com.aerospace.production_resource_service.util.ShiftWindowUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// See MachineReservationServiceImpl for the full explanation of why PESSIMISTIC_WRITE
// row locking (not just @Transactional) is what actually prevents two concurrent
// requests from double-booking the same employee — identical reasoning applies here,
// via EmployeeRepository.findByIdForUpdate.
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeReservationServiceImpl implements EmployeeReservationService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeReservationRepository reservationRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final EmployeeSkillCertificationRepository certificationRepository;

    @Override
    @Transactional
    public EmployeeReservationResponseDTO createReservation(EmployeeReservationRequestDTO dto) {
        validateWindow(dto.getReservedFrom(), dto.getReservedTo());
        Employee employee = employeeRepository.findByIdForUpdate(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        validateEmployeeEligible(employee, dto);
        rejectIfOverlapping(employee.getId(), dto.getReservedFrom(), dto.getReservedTo(), 0L, employee.getEmployeeCode());

        EmployeeReservation reservation = EmployeeReservation.builder()
                .employee(employee)
                .reservedFrom(dto.getReservedFrom())
                .reservedTo(dto.getReservedTo())
                .productionOrderId(dto.getProductionOrderId())
                .createdBy(dto.getCreatedBy())
                .status(ReservationStatus.ACTIVE)
                .build();
        EmployeeReservation saved = reservationRepository.save(reservation);
        log.info("Employee reservation created — employeeCode: {}, {} to {}",
                employee.getEmployeeCode(), saved.getReservedFrom(), saved.getReservedTo());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public EmployeeReservationResponseDTO updateReservation(Long id, EmployeeReservationRequestDTO dto) {
        validateWindow(dto.getReservedFrom(), dto.getReservedTo());
        EmployeeReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot modify a " + reservation.getStatus() + " reservation");
        }
        Employee employee = employeeRepository.findByIdForUpdate(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + dto.getEmployeeId()));

        validateEmployeeEligible(employee, dto);
        rejectIfOverlapping(employee.getId(), dto.getReservedFrom(), dto.getReservedTo(), reservation.getId(), employee.getEmployeeCode());

        reservation.setEmployee(employee);
        reservation.setReservedFrom(dto.getReservedFrom());
        reservation.setReservedTo(dto.getReservedTo());
        reservation.setProductionOrderId(dto.getProductionOrderId());
        EmployeeReservation saved = reservationRepository.save(reservation);
        log.info("Employee reservation updated — id: {}, employeeCode: {}", saved.getId(), employee.getEmployeeCode());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeReservationResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeReservationResponseDTO> listReservations(Long employeeId, LocalDateTime from, LocalDateTime to,
                                                                   Long productionOrderId, String status) {
        List<EmployeeReservation> reservations = employeeId != null
                ? reservationRepository.findByEmployeeIdOrderByReservedFromAsc(employeeId)
                : reservationRepository.findAll();

        return reservations.stream()
                .filter(r -> productionOrderId == null || productionOrderId.equals(r.getProductionOrderId()))
                .filter(r -> status == null || r.getStatus().name().equals(status))
                .filter(r -> from == null || !r.getReservedTo().isBefore(from))
                .filter(r -> to == null || !r.getReservedFrom().isAfter(to))
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void cancelReservation(Long id) {
        EmployeeReservation reservation = findOrThrow(id);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("Employee reservation cancelled — id: {}", id);
    }

    // All 5 employee-side checks from the spec, in order: exists (caller already has
    // the locked row by this point), ACTIVE, qualified (if a skill was specified),
    // no approved-leave conflict, and within the employee's shift window for the
    // WHOLE reserved span (both endpoints, not just the start).
    private void validateEmployeeEligible(Employee employee, EmployeeReservationRequestDTO dto) {
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is not ACTIVE (current: " + employee.getStatus() + ")");
        }

        if (dto.getRequiredSkillId() != null) {
            int minLevel = dto.getMinimumSkillLevel() != null ? dto.getMinimumSkillLevel() : 1;
            List<EmployeeSkillCertification> certs = certificationRepository
                    .findByEmployeeIdAndSkillIdOrderByCreatedAtDesc(employee.getId(), dto.getRequiredSkillId());
            EmployeeSkillCertification latest = certs.isEmpty() ? null : certs.get(0);
            boolean qualified = latest != null
                    && latest.getStatus() != CertificationStatus.REVOKED
                    && (latest.getExpiryDate() == null || !latest.getExpiryDate().isBefore(LocalDate.now()))
                    && latest.getLevel() != null && latest.getLevel() >= minLevel;
            if (!qualified) {
                throw new IllegalStateException(
                        "Employee " + employee.getEmployeeCode() + " does not hold the required skill at level "
                                + minLevel + " or higher for this reservation");
            }
        }

        LocalDate leaveFrom = dto.getReservedFrom().toLocalDate();
        LocalDate leaveTo = dto.getReservedTo().toLocalDate();
        if (employeeLeaveRepository.existsByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee.getId(), LeaveStatus.APPROVED, leaveTo, leaveFrom)) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " has approved leave overlapping this reservation window");
        }

        // A simplification, disclosed: checks both endpoints fall inside a valid
        // shift day+window, rather than modeling multi-day reservations that might
        // span several shift instances. Covers the common single-day-window case.
        if (employee.getShift() != null) {
            boolean fromOk = ShiftWindowUtil.isWithin(employee.getShift(), dto.getReservedFrom());
            boolean toOk = ShiftWindowUtil.isWithin(employee.getShift(), dto.getReservedTo());
            if (!fromOk || !toOk) {
                throw new IllegalStateException(
                        "Employee " + employee.getEmployeeCode() + " is outside their shift window ("
                                + employee.getShift().getName() + ") for the requested reservation");
            }
        }
    }

    // Same purpose as MachineReservationServiceImpl.checkActiveConflict — see there.
    @Override
    @Transactional(readOnly = true)
    public EmployeeReservationResponseDTO checkActiveConflict(Long employeeId, Long excludeProductionOrderId) {
        List<EmployeeReservation> conflicts = reservationRepository.findActiveConflict(
                employeeId, LocalDateTime.now(), excludeProductionOrderId);
        return conflicts.isEmpty() ? null : toDTO(conflicts.get(0));
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Reservation start must be before reservation end");
        }
    }

    private void rejectIfOverlapping(Long employeeId, LocalDateTime from, LocalDateTime to, Long excludeId, String employeeCode) {
        List<EmployeeReservation> conflicts = reservationRepository.findOverlapping(employeeId, from, to, excludeId);
        if (!conflicts.isEmpty()) {
            EmployeeReservation c = conflicts.get(0);
            throw new IllegalStateException(
                    "Employee " + employeeCode + " is already reserved " + c.getReservedFrom() + " to " + c.getReservedTo()
                            + " — overlaps with the requested window");
        }
    }

    private EmployeeReservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }

    private EmployeeReservationResponseDTO toDTO(EmployeeReservation r) {
        return EmployeeReservationResponseDTO.builder()
                .id(r.getId())
                .employeeId(r.getEmployee().getId())
                .employeeCode(r.getEmployee().getEmployeeCode())
                .reservedFrom(r.getReservedFrom())
                .reservedTo(r.getReservedTo())
                .productionOrderId(r.getProductionOrderId())
                .createdBy(r.getCreatedBy())
                .status(r.getStatus().name())
                .version(r.getVersion())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
