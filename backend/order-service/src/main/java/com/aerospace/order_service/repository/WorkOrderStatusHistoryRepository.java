package com.aerospace.order_service.repository;

import com.aerospace.order_service.entity.WorkOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, Long> {
    List<WorkOrderStatusHistory> findByWorkOrderIdOrderByChangedAtDesc(Long workOrderId);
    List<WorkOrderStatusHistory> findByWoNumberOrderByChangedAtDesc(String woNumber);
}