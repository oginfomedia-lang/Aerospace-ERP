package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.CapaRecord;
import com.aerospace.inventory_service.entity.CapaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CapaRepository extends JpaRepository<CapaRecord, Long> {
    List<CapaRecord> findByNcrIdOrderByCreatedAtDesc(Long ncrId);
    List<CapaRecord> findByStatus(CapaStatus status);
    List<CapaRecord> findAllByOrderByCreatedAtDesc();
}
