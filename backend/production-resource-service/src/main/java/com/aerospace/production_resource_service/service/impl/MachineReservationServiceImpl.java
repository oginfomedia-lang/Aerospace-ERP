package com.aerospace.production_resource_service.service.impl;

import com.aerospace.production_resource_service.dto.MachineReservationRequestDTO;
import com.aerospace.production_resource_service.dto.MachineReservationResponseDTO;
import com.aerospace.production_resource_service.entity.Machine;
import com.aerospace.production_resource_service.entity.MachineReservation;
import com.aerospace.production_resource_service.entity.MachineStatus;
import com.aerospace.production_resource_service.entity.ReservationStatus;
import com.aerospace.production_resource_service.exception.ResourceNotFoundException;
import com.aerospace.production_resource_service.repository.MachineRepository;
import com.aerospace.production_resource_service.repository.MachineReservationRepository;
import com.aerospace.production_resource_service.service.MachineReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ── Why @Transactional alone is NOT enough to prevent double-booking ──────────────
// Under the database's default READ_COMMITTED isolation (Postgres's default, and
// this app's default), two concurrent transactions calling createReservation for the
// SAME machine can both run their overlap-check SELECT before either has committed
// its INSERT. Neither sees the other's not-yet-committed row, so both conclude "no
// conflict" and both insert — a classic race condition (this is NOT solved by
// wrapping the method in @Transactional; @Transactional only groups statements into
// one atomic unit, it does not by itself serialize concurrent transactions against
// each other).
//
// The fix used here: PESSIMISTIC_WRITE row locking on the Machine itself
// (MachineRepository.findByIdForUpdate), the same pattern common-lib's
// DocumentSequenceService already uses for sequence-number generation. The first
// transaction to reach findByIdForUpdate acquires an exclusive DB-level lock on that
// Machine row; a second concurrent call for the SAME machine blocks at that same line
// until the first transaction commits or rolls back. Once unblocked, the second
// transaction's overlap-check SELECT now correctly sees the first transaction's
// committed reservation, and correctly rejects if it conflicts. Reservations for
// DIFFERENT machines never block each other, since each locks its own Machine row.
@Service
@RequiredArgsConstructor
@Slf4j
public class MachineReservationServiceImpl implements MachineReservationService {

    private final MachineRepository machineRepository;
    private final MachineReservationRepository reservationRepository;

    @Override
    @Transactional
    public MachineReservationResponseDTO createReservation(MachineReservationRequestDTO dto) {
        validateWindow(dto.getReservedFrom(), dto.getReservedTo());
        Machine machine = machineRepository.findByIdForUpdate(dto.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + dto.getMachineId()));
        if (machine.getStatus() != MachineStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is not ACTIVE (current: " + machine.getStatus() + ")");
        }
        rejectIfOverlapping(machine.getId(), dto.getReservedFrom(), dto.getReservedTo(), 0L, machine.getMachineCode());

        MachineReservation reservation = MachineReservation.builder()
                .machine(machine)
                .reservedFrom(dto.getReservedFrom())
                .reservedTo(dto.getReservedTo())
                .productionOrderId(dto.getProductionOrderId())
                .createdBy(dto.getCreatedBy())
                .status(ReservationStatus.ACTIVE)
                .build();
        MachineReservation saved = reservationRepository.save(reservation);
        log.info("Machine reservation created — machineCode: {}, {} to {}",
                machine.getMachineCode(), saved.getReservedFrom(), saved.getReservedTo());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public MachineReservationResponseDTO updateReservation(Long id, MachineReservationRequestDTO dto) {
        validateWindow(dto.getReservedFrom(), dto.getReservedTo());
        MachineReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot modify a " + reservation.getStatus() + " reservation");
        }
        // Lock the machine (possibly a different one, if machineId changed) before
        // re-checking overlap — same reasoning as createReservation.
        Machine machine = machineRepository.findByIdForUpdate(dto.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + dto.getMachineId()));
        if (machine.getStatus() != MachineStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is not ACTIVE (current: " + machine.getStatus() + ")");
        }
        rejectIfOverlapping(machine.getId(), dto.getReservedFrom(), dto.getReservedTo(), reservation.getId(), machine.getMachineCode());

        reservation.setMachine(machine);
        reservation.setReservedFrom(dto.getReservedFrom());
        reservation.setReservedTo(dto.getReservedTo());
        reservation.setProductionOrderId(dto.getProductionOrderId());
        MachineReservation saved = reservationRepository.save(reservation);
        log.info("Machine reservation updated — id: {}, machineCode: {}", saved.getId(), machine.getMachineCode());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MachineReservationResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineReservationResponseDTO> listReservations(Long machineId, LocalDateTime from, LocalDateTime to,
                                                                  Long productionOrderId, String status) {
        List<MachineReservation> reservations = machineId != null
                ? reservationRepository.findByMachineIdOrderByReservedFromAsc(machineId)
                : reservationRepository.findAll();

        return reservations.stream()
                .filter(r -> productionOrderId == null || productionOrderId.equals(r.getProductionOrderId()))
                .filter(r -> status == null || r.getStatus().name().equals(status))
                .filter(r -> from == null || !r.getReservedTo().isBefore(from))
                .filter(r -> to == null || !r.getReservedFrom().isAfter(to))
                .map(this::toDTO)
                .toList();
    }

    // "Delete" = cancel, not a hard row delete — keeps the booking history intact for
    // planning traceability, same reasoning as ProductionOrder's delete guard.
    @Override
    @Transactional
    public void cancelReservation(Long id) {
        MachineReservation reservation = findOrThrow(id);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        log.info("Machine reservation cancelled — id: {}", id);
    }

    // Called from production-service's startStage (via ProductionResourceClient) right
    // alongside the existing machine-ACTIVE/overdue checks — "at" is computed here,
    // server-side, never trusted from the caller, same principle as every other
    // eligibility check in this app.
    @Override
    @Transactional(readOnly = true)
    public MachineReservationResponseDTO checkActiveConflict(Long machineId, Long excludeProductionOrderId) {
        List<MachineReservation> conflicts = reservationRepository.findActiveConflict(
                machineId, LocalDateTime.now(), excludeProductionOrderId);
        return conflicts.isEmpty() ? null : toDTO(conflicts.get(0));
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Reservation start must be before reservation end");
        }
    }

    // Overlap rule, explained with a timeline: two windows [aStart,aEnd) and
    // [bStart,bEnd) overlap only if aStart < bEnd AND aEnd > bStart. A reservation
    // ending exactly when another starts (10:00-12:00 then 12:00-14:00) does NOT
    // overlap under this rule — back-to-back bookings are allowed, touching boundaries
    // are not a conflict. This is the explicit business rule chosen here.
    private void rejectIfOverlapping(Long machineId, LocalDateTime from, LocalDateTime to, Long excludeId, String machineCode) {
        List<MachineReservation> conflicts = reservationRepository.findOverlapping(machineId, from, to, excludeId);
        if (!conflicts.isEmpty()) {
            MachineReservation c = conflicts.get(0);
            throw new IllegalStateException(
                    "Machine " + machineCode + " is already reserved " + c.getReservedFrom() + " to " + c.getReservedTo()
                            + " — overlaps with the requested window");
        }
    }

    private MachineReservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }

    private MachineReservationResponseDTO toDTO(MachineReservation r) {
        return MachineReservationResponseDTO.builder()
                .id(r.getId())
                .machineId(r.getMachine().getId())
                .machineCode(r.getMachine().getMachineCode())
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
