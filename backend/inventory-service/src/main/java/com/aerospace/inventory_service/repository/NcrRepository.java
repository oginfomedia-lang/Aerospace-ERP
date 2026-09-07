package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.NcrStatus;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NcrRepository extends JpaRepository<NonConformanceReport, Long> {
    List<NonConformanceReport> findByStatus(NcrStatus status);

    // Pessimistic row lock — used by NcrServiceImpl.disposition() so two concurrent
    // disposition requests against the same NCR can't both read status != DISPOSITIONED
    // and both proceed to deduct/return the same rejected stock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NonConformanceReport n where n.id = :id")
    Optional<NonConformanceReport> lockById(@Param("id") Long id);

    // Existence check for Kafka redelivery dedup (Fix 6) — one auto-created NCR per
    // NON_CONFORMING FAI result.
    boolean existsBySourceFaiNumber(String sourceFaiNumber);
}
