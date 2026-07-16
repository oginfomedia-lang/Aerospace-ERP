package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.ApprovalMatrixRequest;
import com.aerospace.procurement_service.entity.ApprovalMatrix;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.repository.ApprovalMatrixRepository;
import com.aerospace.procurement_service.service.ApprovalMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<ApprovalMatrix> getAll(String moduleCode) {
        return moduleCode != null && !moduleCode.isBlank()
                ? repository.findByModuleCodeOrderByLevelSequenceAsc(moduleCode)
                : repository.findAllByOrderByModuleCodeAscLevelSequenceAsc();
    }

    @Override
    @Transactional
    public ApprovalMatrix create(ApprovalMatrixRequest request) {
        ApprovalMatrix matrix = ApprovalMatrix.builder()
                .moduleCode(request.getModuleCode())
                .amountFrom(request.getAmountFrom())
                .amountTo(request.getAmountTo())
                .approverRole(request.getApproverRole())
                .levelSequence(request.getLevelSequence())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return repository.save(matrix);
    }

    @Override
    @Transactional
    public ApprovalMatrix update(Long id, ApprovalMatrixRequest request) {
        ApprovalMatrix matrix = findOrThrow(id);
        matrix.setModuleCode(request.getModuleCode());
        matrix.setAmountFrom(request.getAmountFrom());
        matrix.setAmountTo(request.getAmountTo());
        matrix.setApproverRole(request.getApproverRole());
        matrix.setLevelSequence(request.getLevelSequence());
        if (request.getIsActive() != null) matrix.setActive(request.getIsActive());
        return repository.save(matrix);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(findOrThrow(id));
    }

    private ApprovalMatrix findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval matrix band not found: " + id));
    }
}
