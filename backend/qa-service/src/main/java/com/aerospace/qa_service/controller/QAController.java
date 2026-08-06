package com.aerospace.qa_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.qa_service.common.ApiResponse;
import com.aerospace.qa_service.dto.InspectionRequestDTO;
import com.aerospace.qa_service.dto.InspectionResponseDTO;
import com.aerospace.qa_service.service.InspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
public class QAController {

    private final InspectionService inspectionService;

    // Core endpoint — QA engineer submits inspection decision
    // Saves to DB + publishes to Kafka → inventory auto-updates
    // POST /api/v1/qa/inspect
    @Audited(action = "CREATE", entityType = "Inspection")
    @PostMapping("/inspect")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> submitDecision(
            @Valid @RequestBody InspectionRequestDTO dto) {
        InspectionResponseDTO response = inspectionService.submitDecision(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("QA_DECISION_SUBMITTED", response));
    }

    // Get single inspection by ID
    // GET /api/v1/qa/inspections/5
    @GetMapping("/inspections/{id}")
    public ResponseEntity<ApiResponse<InspectionResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", inspectionService.getById(id)));
    }

    // Full audit trail for a part — all inspections ever made
    // GET /api/v1/qa/part/5/history
    @GetMapping("/part/{partId}/history")
    public ResponseEntity<ApiResponse<List<InspectionResponseDTO>>> getByPart(
            @PathVariable Long partId) {
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", inspectionService.getByPartId(partId)));
    }

    // Full audit trail for a batch — aerospace traceability requirement
    // GET /api/v1/qa/batch/BATCH-2024-001/history
    @GetMapping("/batch/{batchNumber}/history")
    public ResponseEntity<ApiResponse<List<InspectionResponseDTO>>> getByBatch(
            @PathVariable String batchNumber) {
        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", inspectionService.getByBatchNumber(batchNumber)));
    }

    // Manual trigger for retry — useful during testing or after Kafka outage
    // POST /api/v1/qa/retry-failed
    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<String>> retryFailed() {
        inspectionService.retryFailedPublications();
        return ResponseEntity.ok(new ApiResponse<>("RETRY_TRIGGERED",
                "Failed publications retried. Check logs for results."));
    }
}