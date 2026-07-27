package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.CapaHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CapaHistoryRepository extends JpaRepository<CapaHistory, Long> {
    List<CapaHistory> findByCapaRecordIdOrderByOccurredAtAsc(Long capaRecordId);
}
