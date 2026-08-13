package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.SalesOrder;
import com.aerospace.sales_service.entity.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    List<SalesOrder> findByIsCurrentRevisionTrue();
    List<SalesOrder> findByCustomerIdAndIsCurrentRevisionTrue(Long customerId);
    List<SalesOrder> findByStatus(SalesOrderStatus status);
    Optional<SalesOrder> findByOrderNumberAndIsCurrentRevisionTrue(String orderNumber);
    List<SalesOrder> findByOrderNumberOrderByRevisionNumberDesc(String orderNumber); // full revision history, used later
}