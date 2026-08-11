package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.EmployeeReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeReservationRepository extends JpaRepository<EmployeeReservation, Long> {
    List<EmployeeReservation> findByEmployeeIdOrderByReservedFromAsc(Long employeeId);
    List<EmployeeReservation> findByProductionOrderId(Long productionOrderId);

    @Query("select r from EmployeeReservation r where r.employee.id = :employeeId " +
            "and r.status = com.aerospace.production_resource_service.entity.ReservationStatus.ACTIVE " +
            "and r.id <> :excludeId " +
            "and r.reservedFrom < :to and r.reservedTo > :from")
    List<EmployeeReservation> findOverlapping(@Param("employeeId") Long employeeId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to,
                                               @Param("excludeId") Long excludeId);

    // Same purpose as MachineReservationRepository.findActiveConflict — see there.
    @Query("select r from EmployeeReservation r where r.employee.id = :employeeId " +
            "and r.status = com.aerospace.production_resource_service.entity.ReservationStatus.ACTIVE " +
            "and r.reservedFrom <= :at and r.reservedTo > :at " +
            "and (r.productionOrderId is null or r.productionOrderId <> :excludeProductionOrderId)")
    List<EmployeeReservation> findActiveConflict(@Param("employeeId") Long employeeId,
                                                  @Param("at") LocalDateTime at,
                                                  @Param("excludeProductionOrderId") Long excludeProductionOrderId);
}
