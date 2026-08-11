package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Machine;
import com.aerospace.production_resource_service.entity.MachineStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByStatus(MachineStatus status);
    List<Machine> findByNextMaintenanceDueAtBefore(LocalDateTime cutoff);
    boolean existsByMachineCode(String machineCode);

    // Locks this Machine row for the rest of the transaction — used by
    // MachineReservationServiceImpl.createReservation/updateReservation so two
    // concurrent reservation requests for the SAME machine can't both pass the
    // overlap check before either commits. See that class for the full explanation.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Machine m where m.id = :id")
    Optional<Machine> findByIdForUpdate(@Param("id") Long id);
}
