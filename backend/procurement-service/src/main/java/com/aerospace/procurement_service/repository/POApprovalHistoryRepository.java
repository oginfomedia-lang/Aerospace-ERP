package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.ApprovalActionStatus;
import com.aerospace.procurement_service.entity.POApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface POApprovalHistoryRepository extends JpaRepository<POApprovalHistory, Long> {

    List<POApprovalHistory> findByPurchaseOrder_IdOrderByDocumentVersionDescApprovalLevelAsc(Long purchaseOrderId);

    Optional<POApprovalHistory> findByPurchaseOrder_IdAndDocumentVersionAndApprovalLevel(
            Long purchaseOrderId, int documentVersion, int approvalLevel);

    List<POApprovalHistory> findByPurchaseOrder_IdAndDocumentVersion(Long purchaseOrderId, int documentVersion);

    Optional<POApprovalHistory> findByPurchaseOrder_IdAndDocumentVersionAndStatus(
            Long purchaseOrderId, int documentVersion, ApprovalActionStatus status);
}
