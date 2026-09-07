package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    // Traceability & Logic
    Optional<StockBalance> findByPartIdAndBatchNumberAndStatus(
            Long partId, String batchNumber, StockStatus status);

    // Pessimistic row lock — must be called (inside an existing @Transactional method)
    // immediately before mutating a StockBalance's quantity, and the quantity re-validated
    // against THIS freshly-locked read, not any earlier unlocked one. A second concurrent
    // caller blocks on the DB row lock until the first transaction commits, then sees the
    // updated quantity — closing the oversell/lost-update race on concurrent decrements.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StockBalance s where s.id = :id")
    Optional<StockBalance> lockById(@Param("id") Long id);

    // Existence check for Kafka redelivery dedup (Fix 5) — one StockBalance batch per
    // completed production order.
    boolean existsBySourceProductionOrderRef(String sourceProductionOrderRef);

    // Bin-to-Bin Transfers
    Optional<StockBalance> findByPartIdAndBatchNumberAndLocationAndStatus(
            Long partId, String batchNumber, String location, StockStatus status);

    // Per-part approved quantity (used in PartService)
    @Query("SELECT SUM(s.quantity) FROM StockBalance s WHERE s.partId = :partId AND s.status = 'APPROVED'")
    Long getAvailableQuantity(@Param("partId") Long partId);

    // Warehouse-wide approved total (used in DashboardService)
    // Replaces the inefficient findAll().stream().sum() pattern
    @Query("SELECT SUM(s.quantity) FROM StockBalance s WHERE s.status = 'APPROVED'")
    Long getTotalApprovedStock();

    @Query("SELECT SUM(s.quantity) FROM StockBalance s WHERE s.status = 'PENDING_QA'")
    Long getTotalPendingQA();

    @Query("SELECT COUNT(s) FROM StockBalance s WHERE s.status = 'REJECTED' AND s.quantity > 0")
    Long countRejectedLots();

    // Per-part approved quantity, warehouse-wide — used by DashboardService to compute
    // low-stock count without an N+1 query per part.
    @Query("SELECT s.partId, SUM(s.quantity) FROM StockBalance s WHERE s.status = 'APPROVED' GROUP BY s.partId")
    List<Object[]> getApprovedQuantityByPart();

    // Warehouse-wide approved stock value. StockBalance.partId is a bare Long (no
    // mapped association to Part), so this is an implicit theta-join rather than
    // "s.part.unitPrice". Parts with no unitPrice set are excluded rather than treated
    // as 0 — an unpriced part is unknown value, not zero value.
    @Query("SELECT COALESCE(SUM(s.quantity * p.unitPrice), 0) FROM StockBalance s, Part p " +
           "WHERE p.id = s.partId AND s.status = 'APPROVED' AND p.unitPrice IS NOT NULL")
    BigDecimal getTotalApprovedInventoryValue();

    @Query("SELECT s FROM StockBalance s WHERE s.partId = :partId")
    List<StockBalance> findAllBalancesByPart(@Param("partId") Long partId);

    // Automated Expiry Scheduler
    List<StockBalance> findAllByExpiryDateBeforeAndStatus(LocalDateTime dateTime, StockStatus status);

    // Bin transfer convenience method — always targets APPROVED stock
    default Optional<StockBalance> findByPartIdAndBatchNumberAndLocation(
            Long partId, String batchNumber, String location) {
        return findByPartIdAndBatchNumberAndLocationAndStatus(
                partId, batchNumber, location, StockStatus.APPROVED);
    }

    List<StockBalance> findAllByPartIdAndBatchNumberAndStatusOrderByIdAsc(
        Long partId, 
        String batchNumber, 
        StockStatus status
        );
}