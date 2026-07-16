package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.ApprovalMatrixRequest;
import com.aerospace.procurement_service.entity.ApprovalMatrix;

import java.math.BigDecimal;
import java.util.List;

public interface ApprovalMatrixService {
    // PUR-BR-001: returns every qualifying band (all lower levels + the matching band),
    // ordered by level ascending. Empty means the amount needs no approval at all.
    List<ApprovalMatrix> evaluateChain(String moduleCode, BigDecimal amount);

    // ── Admin management (previously only the hardcoded ApprovalMatrixSeeder could
    // populate this table) ───────────────────────────────────────────────────────
    List<ApprovalMatrix> getAll(String moduleCode);
    ApprovalMatrix create(ApprovalMatrixRequest request);
    ApprovalMatrix update(Long id, ApprovalMatrixRequest request);
    void delete(Long id);
}
