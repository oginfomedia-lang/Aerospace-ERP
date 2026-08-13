package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.Dispatch;
import com.aerospace.sales_service.entity.DispatchStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findBySalesOrder_Id(Long salesOrderId);
    List<Dispatch> findByStatus(DispatchStatus status);
}
