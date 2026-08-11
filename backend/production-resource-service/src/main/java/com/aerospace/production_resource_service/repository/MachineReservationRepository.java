package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.MachineReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MachineReservationRepository extends JpaRepository<MachineReservation, Long> {
    List<MachineReservation> findByMachineIdOrderByReservedFromAsc(Long machineId);
    List<MachineReservation> findByProductionOrderId(Long productionOrderId);

    // Overlap rule: newStart < existingEnd AND newEnd > existingStart. Only ACTIVE
    // reservations count — a CANCELLED one never blocks a new booking. Excludes the
    // reservation being updated itself via :excludeId (pass 0 / any non-matching id
    // when creating, since there's nothing to exclude yet).
    @Query("select r from MachineReservation r where r.machine.id = :machineId " +
            "and r.status = com.aerospace.production_resource_service.entity.ReservationStatus.ACTIVE " +
            "and r.id <> :excludeId " +
            "and r.reservedFrom < :to and r.reservedTo > :from")
    List<MachineReservation> findOverlapping(@Param("machineId") Long machineId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              @Param("excludeId") Long excludeId);

    // Used by startStage's reservation-aware check: is there an ACTIVE reservation for
    // this machine covering RIGHT NOW that belongs to a DIFFERENT production order? An
    // unlinked reservation (productionOrderId is null) always blocks, since there's no
    // way to know it "belongs" to the order trying to start.
    @Query("select r from MachineReservation r where r.machine.id = :machineId " +
            "and r.status = com.aerospace.production_resource_service.entity.ReservationStatus.ACTIVE " +
            "and r.reservedFrom <= :at and r.reservedTo > :at " +
            "and (r.productionOrderId is null or r.productionOrderId <> :excludeProductionOrderId)")
    List<MachineReservation> findActiveConflict(@Param("machineId") Long machineId,
                                                 @Param("at") LocalDateTime at,
                                                 @Param("excludeProductionOrderId") Long excludeProductionOrderId);
}
