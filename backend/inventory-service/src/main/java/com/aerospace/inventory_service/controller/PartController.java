package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.dto.PartRequestDTO;
import com.aerospace.inventory_service.dto.PartResponseDTO;
import com.aerospace.inventory_service.service.PartService;
import com.aerospace.inventory_service.service.TransactionService;
import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.entity.InventoryTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;           // FIX 1: was org.hibernate.query.Page
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class PartController {

    private final PartService partService;
    private final TransactionService transactionService; 

    @PostMapping
    public ResponseEntity<ApiResponse<PartResponseDTO>> create(@RequestBody PartRequestDTO dto) {
        PartResponseDTO response = partService.createPart(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponseDTO>> update(
            @PathVariable Long id, @RequestBody PartRequestDTO dto) {
        PartResponseDTO response = partService.updatePart(id, dto);
        return ResponseEntity.ok(new ApiResponse<>("UPDATED", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PartResponseDTO>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partService.getAllParts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partService.getById(id)));
    }

    @GetMapping("/sku/{skuCode}")
    public ResponseEntity<ApiResponse<PartResponseDTO>> getBySkuCode(@PathVariable String skuCode) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partService.getBySkuCode(skuCode)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        partService.deletePart(id);
        return ResponseEntity.ok(new ApiResponse<>("DELETED", "Part removed successfully"));
    }

    // ERP Step 1+2: Receive Stock & Auto-Generate GRN (F-36 GRN Form)
    // GRN-FR-001: the GRN number is always system-generated; any `grnNumber` passed here
    // is accepted for backward compatibility but ignored — see PartResponseDTO.grnNumber
    // in the response for the authoritative, system-assigned number.
    @PostMapping("/{id}/inward-from-grn")
    public ResponseEntity<ApiResponse<PartResponseDTO>> inwardStock(
            @PathVariable Long id,
            @RequestParam int qty,
            @RequestParam String batchNumber,
            @RequestParam String location,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime expiryDate,
            // GRN traceability fields (F-36 GRN form)
            @RequestParam(required = false) String grnNumber,
            @RequestParam(required = false) String challanNumber,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String transportName,
            @RequestParam(required = false) Integer inspectionQty,
            @RequestParam(required = false) String poNumber) {
        PartResponseDTO result = partService.addStock(id, qty, batchNumber, location, expiryDate,
                grnNumber, challanNumber, supplierName, transportName, inspectionQty, poNumber);
        return ResponseEntity.ok(new ApiResponse<>(
                "STOCK_RECEIVED_" + result.getStatus(), result));
    }
    
    // ERP Step: Issue to Production
    // batchNumber is optional: omit it to auto-consume the oldest APPROVED batches
    // first (FIFO), splitting across as many batches as needed; pass it to pin the
    // issue to one specific batch instead (fails if that batch alone is short).
    @PostMapping("/{id}/issue-to-production")
    public ResponseEntity<ApiResponse<PartResponseDTO>> issueMaterial(
            @PathVariable Long id,
            @RequestParam int qty,
            @RequestParam(required = false) String batchNumber,
            // Set when this issue is against a specific Work Order material line —
            // lets order-service/production-service update their own records for
            // this same issuance. Omit for ad-hoc issues not tied to any WO.
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) String woNumber,
            @RequestParam(required = false) Long lineItemId) {
        return ResponseEntity.ok(new ApiResponse<>("MATERIAL_ISSUED_TO_PRODUCTION",
                partService.issueToProduction(id, qty, batchNumber, workOrderId, woNumber, lineItemId)));
    }

    // ERP Step: QA Lifecycle
    @PostMapping("/{id}/qa-decision")
    public ResponseEntity<ApiResponse<PartResponseDTO>> processQA(
            @PathVariable Long id,
            @RequestParam String batchNumber,
            @RequestParam String status,
            @RequestParam String remarks) {
        return ResponseEntity.ok(new ApiResponse<>("QA_STATUS_UPDATED",
                partService.processQADecision(id, batchNumber, status, remarks)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PartResponseDTO>>> search(@RequestParam String name) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", partService.searchByName(name)));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getTransactionHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                partService.getTransactionHistory(id)));
    }

    // Batch traceability — auditors
    @GetMapping("/transactions/batch/{batchNumber}")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getBatchHistory(
            @PathVariable String batchNumber) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                transactionService.getByBatchNumber(batchNumber))); // FIX 2: through service layer
    }

    // Paginated parts list
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<PartResponseDTO>>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                partService.getAllParts(PageRequest.of(page, size))));
    }
}