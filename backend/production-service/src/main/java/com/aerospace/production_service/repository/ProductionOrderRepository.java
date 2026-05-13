package com.aerospace.production_service.repository;

import com.aerospace.production_service.entity.ProductionOrder;
import com.aerospace.production_service.entity.ProductionOrderStatus;
import com.aerospace.production_service.entity.ProductionPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<ProductionOrder> findByStatus(ProductionOrderStatus status);

    List<ProductionOrder> findByPriority(ProductionPriority priority);

    List<ProductionOrder> findByProductionLine(String productionLine);

    List<ProductionOrder> findByAssignedTeam(String assignedTeam);

    List<ProductionOrder> findByProductNameContainingIgnoreCase(String productName);

    Page<ProductionOrder> findByStatus(ProductionOrderStatus status, Pageable pageable);

    // Overdue orders: planned end passed, still not completed/cancelled
    @Query("""
        SELECT o FROM ProductionOrder o
        WHERE o.plannedEndDate < :now
          AND o.status NOT IN ('COMPLETED', 'CANCELLED')
    """)
    List<ProductionOrder> findOverdueOrders(@Param("now") LocalDateTime now);

    // Orders starting soon (within next N hours) that are still in PLANNED/RELEASED
    @Query("""
        SELECT o FROM ProductionOrder o
        WHERE o.plannedStartDate BETWEEN :from AND :to
          AND o.status IN ('PLANNED', 'RELEASED')
    """)
    List<ProductionOrder> findOrdersStartingSoon(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Dashboard aggregation counts
    @Query("SELECT COUNT(o) FROM ProductionOrder o WHERE o.status = :status")
    long countByStatus(@Param("status") ProductionOrderStatus status);

    @Query("SELECT SUM(o.plannedQuantity) FROM ProductionOrder o")
    Long sumPlannedQuantity();

    @Query("SELECT SUM(o.producedQuantity) FROM ProductionOrder o")
    Long sumProducedQuantity();

    @Query("SELECT SUM(o.rejectedQuantity) FROM ProductionOrder o")
    Long sumRejectedQuantity();

    @Query("SELECT o.priority, COUNT(o) FROM ProductionOrder o GROUP BY o.priority")
    List<Object[]> countGroupedByPriority();

    @Query("SELECT o.status, COUNT(o) FROM ProductionOrder o GROUP BY o.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT o.productName, COUNT(o) FROM ProductionOrder o GROUP BY o.productName")
    List<Object[]> countGroupedByProduct();
}
