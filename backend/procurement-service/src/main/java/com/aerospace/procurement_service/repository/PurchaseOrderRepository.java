package com.aerospace.procurement_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aerospace.procurement_service.entity.PurchaseOrder;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
   Optional<PurchaseOrder> findByPoNumber(String poNumber);
   //for the supplier portal - a supplier's own POs, most recent first
   List<PurchaseOrder> findBySupplier_IdOrderByOrderDateDesc(Long supplierId);
}
