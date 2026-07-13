package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.NcrStatus;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NcrRepository extends JpaRepository<NonConformanceReport, Long> {
    List<NonConformanceReport> findByStatus(NcrStatus status);
}
