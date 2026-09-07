package com.aerospace.common.approval;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval-matrix")
@RequiredArgsConstructor
public class ApprovalMatrixController {

    private final ApprovalMatrixRepository repository;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<ApprovalMatrix>> getAll(@RequestParam(required = false) String moduleCode) {
        List<ApprovalMatrix> result = (moduleCode != null && !moduleCode.isBlank())
                ? repository.findByModuleCodeOrderByLevelSequenceAsc(moduleCode)
                : repository.findAllByOrderByModuleCodeAscLevelSequenceAsc();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<ApprovalMatrix> create(@Valid @RequestBody ApprovalMatrixRequest request) {
        ApprovalMatrix matrix = ApprovalMatrix.builder()
                .moduleCode(request.getModuleCode())
                .amountFrom(request.getAmountFrom())
                .amountTo(request.getAmountTo())
                .approverRole(request.getApproverRole())
                .levelSequence(request.getLevelSequence())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .escalationHours(request.getEscalationHours())
                .escalationToRole(request.getEscalationToRole())
                .approverUsername(request.getApproverUsername())
                .build();
        return ResponseEntity.ok(repository.save(matrix));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApprovalMatrix> update(@PathVariable Long id, @Valid @RequestBody ApprovalMatrixRequest request) {
        ApprovalMatrix matrix = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval matrix band not found: " + id));
        matrix.setModuleCode(request.getModuleCode());
        matrix.setAmountFrom(request.getAmountFrom());
        matrix.setAmountTo(request.getAmountTo());
        matrix.setApproverRole(request.getApproverRole());
        matrix.setLevelSequence(request.getLevelSequence());
        if (request.getIsActive() != null) matrix.setActive(request.getIsActive());
        matrix.setEscalationHours(request.getEscalationHours());
        matrix.setEscalationToRole(request.getEscalationToRole());
        matrix.setApproverUsername(request.getApproverUsername());
        return ResponseEntity.ok(repository.save(matrix));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}