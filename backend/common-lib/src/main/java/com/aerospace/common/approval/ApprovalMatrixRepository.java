package com.aerospace.common.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ApprovalMatrixRepository extends JpaRepository<ApprovalMatrix, Long> {

    List<ApprovalMatrix> findByModuleCodeAndIsActiveTrueAndAmountFromLessThanEqualOrderByLevelSequenceAsc(
            String moduleCode, BigDecimal amount);

    List<ApprovalMatrix> findByModuleCodeOrderByLevelSequenceAsc(String moduleCode);

    List<ApprovalMatrix> findAllByOrderByModuleCodeAscLevelSequenceAsc();

    long countByModuleCode(String moduleCode);
}