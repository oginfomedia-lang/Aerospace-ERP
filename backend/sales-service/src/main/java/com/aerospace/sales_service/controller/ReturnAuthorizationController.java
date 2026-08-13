package com.aerospace.sales_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.InspectReturnRequestDTO;
import com.aerospace.sales_service.dto.ReturnAuthorizationRequestDTO;
import com.aerospace.sales_service.dto.ReturnAuthorizationResponseDTO;
import com.aerospace.sales_service.entity.ReturnDisposition;
import com.aerospace.sales_service.service.ReturnAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales/rma")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_EXECUTIVE','SALES_MANAGER','WAREHOUSE_OPERATOR','MANAGER','ADMIN')")
public class ReturnAuthorizationController {

    private final ReturnAuthorizationService returnAuthorizationService;

    @Audited(action = "AUTHORIZE", entityType = "ReturnAuthorization")
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnAuthorizationResponseDTO>> authorize(
            @Valid @RequestBody ReturnAuthorizationRequestDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("RMA_AUTHORIZED",
                returnAuthorizationService.authorizeReturn(dto, auth.getName())));
    }

    @Audited(action = "RECEIVE", entityType = "ReturnAuthorization")
    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<ReturnAuthorizationResponseDTO>> receive(
            @PathVariable Long id, @RequestParam String location) {
        return ResponseEntity.ok(new ApiResponse<>("RMA_RECEIVED",
                returnAuthorizationService.receiveReturn(id, location)));
    }

    // Inspecting a return is the same skill as any other QA release — reuses
    // QA_INSPECTOR rather than inventing a new role, alongside SALES_MANAGER per §18.11.
    @Audited(action = "INSPECT", entityType = "ReturnAuthorization")
    @PreAuthorize("hasAnyRole('QA_INSPECTOR','SALES_MANAGER','MANAGER','ADMIN')")
    @PostMapping("/{id}/inspect")
    public ResponseEntity<ApiResponse<ReturnAuthorizationResponseDTO>> inspect(
            @PathVariable Long id, @Valid @RequestBody InspectReturnRequestDTO dto, Authentication auth) {
        ReturnDisposition disposition = ReturnDisposition.valueOf(dto.getDisposition().toUpperCase());
        return ResponseEntity.ok(new ApiResponse<>("RMA_INSPECTED",
                returnAuthorizationService.inspectAndDispose(id, disposition, auth.getName(), dto.getNotes())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnAuthorizationResponseDTO>>> list(
            @RequestParam(required = false) String disposition) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", returnAuthorizationService.listReturns(disposition)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnAuthorizationResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", returnAuthorizationService.getById(id)));
    }
}
