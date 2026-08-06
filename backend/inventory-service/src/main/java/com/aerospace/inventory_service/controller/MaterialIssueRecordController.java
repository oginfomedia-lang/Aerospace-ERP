package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.MaterialIssueRecordResponse;
import com.aerospace.inventory_service.entity.MaterialIssueRecord;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.mapper.MaterialIssueRecordMapper;
import com.aerospace.inventory_service.repository.MaterialIssueRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// Printable record of exactly which batches fed a material issue — the outbound
// counterpart to MaterialReceiptController.
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory/material-issue-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STORE_KEEPER','PRODUCTION_ENGINEER','QA_INSPECTOR','MANAGER','ADMIN')")
public class MaterialIssueRecordController {

    private final MaterialIssueRecordRepository repository;
    private final MaterialIssueRecordMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialIssueRecordResponse>> getById(@PathVariable Long id) {
        MaterialIssueRecord record = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material Issue Record not found: " + id));
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", mapper.toDTO(record)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialIssueRecordResponse>>> search(
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) Long lineItemId,
            @RequestParam(required = false) Long partId) {
        List<MaterialIssueRecord> records;
        if (lineItemId != null) {
            records = repository.findByLineItemId(lineItemId);
        } else if (workOrderId != null) {
            records = repository.findByWorkOrderId(workOrderId);
        } else if (partId != null) {
            records = repository.findByPartId(partId);
        } else {
            records = repository.findAll();
        }
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                records.stream().map(mapper::toDTO).collect(Collectors.toList())));
    }
}
