package com.aerospace.procurement_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerospace.procurement_service.entity.RequestForQuotation;


public interface RequestForQuotationRepository extends JpaRepository<RequestForQuotation, Long> {
    List<RequestForQuotation> findAllByOrderByCreatedAtDesc();
    //for the supplier portal - RFQs a given supplier was invited to
    List<RequestForQuotation> findByInvitedSuppliers_Id(Long supplierId);
    //for linking a PR row straight to its RFQ
    List<RequestForQuotation> findByPurchaseRequisition_IdOrderByCreatedAtDesc(Long purchaseRequisitionId);

}
