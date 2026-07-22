package com.aerospace.inventory_service.repository;

import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    // Traceability & Logic
    Optional<StockBalance> findByPartIdAndBatchNumberAndStatus(
            Long partId, String batchNumber, StockStatus status);

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