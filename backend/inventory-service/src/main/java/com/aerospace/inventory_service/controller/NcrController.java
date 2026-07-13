package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.common.ApiResponse;
import com.aerospace.inventory_service.dto.DispositionRequest;
import com.aerospace.inventory_service.dto.NcrResponse;
import com.aerospace.inventory_service.service.NcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Non-Conformance Reports — auto-generated when QA rejects a batch
// (PartServiceImpl.processQADecision). A rejected batch stays blocked until its
// NCR is dispositioned here.
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory/ncr")
@RequiredArgsConstructor
public class NcrController {

    private final NcrService ncrService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NcrResponse>>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", ncrService.getAll(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NcrResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", ncrService.getById(id)));
    }

    @PostMapping("/{id}/disposition")
    public ResponseEntity<ApiResponse<NcrResponse>> disposition(
            @PathVariable Long id, @RequestBody DispositionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("DISPOSITIONED", ncrService.disposition(id, request)));
    }
}
