package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findBySalesOrder_Id(Long salesOrderId);
}
