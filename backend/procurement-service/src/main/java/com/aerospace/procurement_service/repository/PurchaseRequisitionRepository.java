package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.PRStatus;
import com.aerospace.procurement_service.entity.PurchaseRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long> {
    List<PurchaseRequisition> findByPartIdAndStatusNotIn(Long partId, List<PRStatus> terminalStatuses);
    List<PurchaseRequisition> findAllByOrderByCreatedAtDesc();
}
