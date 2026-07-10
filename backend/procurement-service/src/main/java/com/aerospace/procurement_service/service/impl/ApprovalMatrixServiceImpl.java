package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.entity.ApprovalMatrix;
import com.aerospace.procurement_service.repository.ApprovalMatrixRepository;
import com.aerospace.procurement_service.service.ApprovalMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalMatrixServiceImpl implements ApprovalMatrixService {

    private final ApprovalMatrixRepository repository;

    @Override
    public List<ApprovalMatrix> evaluateChain(String moduleCode, BigDecimal amount) {
        return repository.findByModuleCodeAndIsActiveTrueAndAmountFromLessThanEqualOrderByLevelSequenceAsc(
                moduleCode, amount);
    }
}
