package com.aerospace.sales_service.repository;

import com.aerospace.sales_service.entity.AllocationStatus;
import com.aerospace.sales_service.entity.SalesOrderAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesOrderAllocationRepository extends JpaRepository<SalesOrderAllocation, Long> {
    List<SalesOrderAllocation> findBySalesOrderLineIdAndStatus(Long lineId, AllocationStatus status);

    // Every allocation across every line of an order — used by the tracking rollup (§18.12).
    List<SalesOrderAllocation> findBySalesOrderLine_SalesOrder_Id(Long salesOrderId);

    // Used by AtpServiceImpl — "how much of this part is currently promised to ANY
    // active sales order, anywhere". A derived query can't easily do a filtered sum,
    // so this one is a real @Query.
    @Query("SELECT COALESCE(SUM(a.quantity), 0) FROM SalesOrderAllocation a " +
           "WHERE a.partId = :partId AND a.status = com.aerospace.sales_service.entity.AllocationStatus.ACTIVE")
    int sumActiveQuantityByPartId(@Param("partId") Long partId);
}
