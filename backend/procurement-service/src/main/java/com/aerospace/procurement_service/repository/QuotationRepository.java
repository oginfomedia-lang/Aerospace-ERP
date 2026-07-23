package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRfq_IdOrderByUnitPriceAsc(Long rfqId);
    Optional<Quotation> findByRfq_IdAndSupplier_Id(Long rfqId, Long supplierId);
    List<Quotation> findBySupplier_IdOrderBySubmittedAtDesc(Long supplierId);
}