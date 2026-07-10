package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.ApprovalMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ApprovalMatrixRepository extends JpaRepository<ApprovalMatrix, Long> {

    // Mirrors the doc's EvaluatePurchaseOrderApprovalChain algorithm: every band whose
    // amountFrom qualifies (not just the exact band) is included, ordered by level —
    // this is what forces a high-value PO through every lower level first (PUR-BR-001).
    List<ApprovalMatrix> findByModuleCodeAndIsActiveTrueAndAmountFromLessThanEqualOrderByLevelSequenceAsc(
            String moduleCode, BigDecimal amount);

    List<ApprovalMatrix> findByModuleCodeAndIsActiveTrueOrderByLevelSequenceAsc(String moduleCode);

    long countByModuleCode(String moduleCode);
}
