package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.ApprovalMatrixRequest;
import com.aerospace.procurement_service.entity.ApprovalMatrix;
import com.aerospace.procurement_service.service.ApprovalMatrixService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin management of the approval-matrix bands used by PurchaseOrderServiceImpl.
// submitForApproval — previously the only way data entered this table was the
// hardcoded ApprovalMatrixSeeder; this is what makes it genuinely configurable.
@RestController
@RequestMapping("/api/approval-matrix")
@RequiredArgsConstructor
public class ApprovalMatrixController {

    private final ApprovalMatrixService approvalMatrixService;

    @GetMapping
    public ResponseEntity<List<ApprovalMatrix>> getAll(
            @RequestParam(required = false) String moduleCode) {
        return ResponseEntity.ok(approvalMatrixService.getAll(moduleCode));
    }

    @PostMapping
    public ResponseEntity<ApprovalMatrix> create(@Valid @RequestBody ApprovalMatrixRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalMatrixService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApprovalMatrix> update(@PathVariable Long id, @Valid @RequestBody ApprovalMatrixRequest request) {
        return ResponseEntity.ok(approvalMatrixService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        approvalMatrixService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
