package com.aerospace.qa_service.controller;

import com.aerospace.qa_service.common.ApiResponse;
import com.aerospace.qa_service.dto.CocGenerateRequest;
import com.aerospace.qa_service.dto.CocResponse;
import com.aerospace.qa_service.service.CocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/qa/coc")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('QA_INSPECTOR','MANAGER','ADMIN')")
public class CocController {

    private final CocService cocService;

    @PostMapping
    public ResponseEntity<ApiResponse<CocResponse>> generate(@Valid @RequestBody CocGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("COC_ISSUED", cocService.generate(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CocResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", cocService.getById(id)));
    }

    @GetMapping("/batch/{batchNumber}")
    public ResponseEntity<ApiResponse<List<CocResponse>>> getByBatch(@PathVariable String batchNumber) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", cocService.getByBatch(batchNumber)));
    }

    @GetMapping("/part/{partId}")
    public ResponseEntity<ApiResponse<List<CocResponse>>> getByPart(@PathVariable Long partId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", cocService.getByPart(partId)));
    }
}
