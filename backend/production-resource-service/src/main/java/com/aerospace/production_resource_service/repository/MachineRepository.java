package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.Machine;
import com.aerospace.production_resource_service.entity.MachineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByStatus(MachineStatus status);
    List<Machine> findByNextMaintenanceDueAtBefore(LocalDateTime cutoff);
    boolean existsByMachineCode(String machineCode);
}
