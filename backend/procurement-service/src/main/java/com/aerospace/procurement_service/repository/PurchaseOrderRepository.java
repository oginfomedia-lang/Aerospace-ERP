package com.aerospace.procurement_service.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aerospace.procurement_service.entity.POStatus;
import com.aerospace.procurement_service.entity.PurchaseOrder;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
   Optional<PurchaseOrder> findByPoNumber(String poNumber);
   //for the supplier portal - a supplier's own POs, most recent first
   List<PurchaseOrder> findBySupplier_IdOrderByOrderDateDesc(Long supplierId);

   // Overdue-delivery sweep: PO passed its delivery date, hasn't been fully received,
   // and hasn't already been flagged once (see PurchaseOrder.overdueNotifiedAt).
   List<PurchaseOrder> findByDeliveryDateBeforeAndOverdueNotifiedAtIsNullAndStatusIn(
           java.time.LocalDateTime now, List<POStatus> statuses);

   // Row-locked read for submit/approve/reject — prevents two rapid clicks (or a retry)
   // from both reading the same pre-transition status and creating duplicate approval chains.
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select po from PurchaseOrder po where po.id = :id")
   Optional<PurchaseOrder> lockById(@Param("id") Long id);
}
