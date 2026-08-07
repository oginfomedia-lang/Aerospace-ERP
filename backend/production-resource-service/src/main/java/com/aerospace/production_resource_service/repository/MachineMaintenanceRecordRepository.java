package com.aerospace.production_resource_service.repository;

import com.aerospace.production_resource_service.entity.MachineMaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineMaintenanceRecordRepository extends JpaRepository<MachineMaintenanceRecord, Long> {
    List<MachineMaintenanceRecord> findByMachineIdOrderByPerformedAtDesc(Long machineId);
}
