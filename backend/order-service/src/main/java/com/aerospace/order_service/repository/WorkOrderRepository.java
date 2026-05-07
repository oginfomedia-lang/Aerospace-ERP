package com.aerospace.order_service.repository;

import com.aerospace.order_service.entity.WorkOrder;
import com.aerospace.order_service.entity.WorkOrderStatus;
import com.aerospace.order_service.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByWoNumber(String woNumber);

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    List<WorkOrder> findByPriority(Priority priority);

    List<WorkOrder> findByAircraftId(String aircraftId);

    List<WorkOrder> findByAssignedTo(String assignedTo);

    List<WorkOrder> findByStatusOrderByCreatedAtDesc(WorkOrderStatus status);

    // WOs past due date that are still active
    @Query("SELECT w FROM WorkOrder w WHERE w.dueDate < :now " +
           "AND w.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<WorkOrder> findOverdue(@Param("now") LocalDateTime now);

    // For QA rejection consumer — find IN_PROGRESS WOs using a specific batch
    @Query("SELECT DISTINCT w FROM WorkOrder w JOIN w.materialLineItems m " +
           "WHERE w.status = :status AND m.batchNumber = :batchNumber")
    List<WorkOrder> findByStatusAndMaterialLineItems_BatchNumber(
            @Param("status") WorkOrderStatus status,
            @Param("batchNumber") String batchNumber);

    // Count by status — for dashboard stats
    long countByStatus(WorkOrderStatus status);

    long countByPriority(Priority priority);

    // Search by aircraft or task description
    @Query("SELECT w FROM WorkOrder w WHERE " +
           "LOWER(w.aircraftId) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(w.taskDescription) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(w.woNumber) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<WorkOrder> search(@Param("q") String query);

    List<WorkOrder> findTop10ByOrderByCreatedAtDesc();
}