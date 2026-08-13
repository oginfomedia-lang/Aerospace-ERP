package com.aerospace.sales_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.QuotationRequestDTO;
import com.aerospace.sales_service.dto.QuotationResponseDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;
import com.aerospace.sales_service.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales/quotations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_EXECUTIVE','SALES_MANAGER','MANAGER','ADMIN')")
public class QuotationController {

    private final QuotationService quotationService;

    @Audited(action = "CREATE", entityType = "Quotation")
    @PostMapping
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> create(
            @Valid @RequestBody QuotationRequestDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("QUOTATION_CREATED", quotationService.createQuotation(dto, auth.getName())));
    }

    @Audited(action = "UPDATE", entityType = "Quotation")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody QuotationRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("QUOTATION_UPDATED", quotationService.updateQuotation(id, dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuotationResponseDTO>>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", quotationService.listQuotations(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", quotationService.getQuotationById(id)));
    }

    @Audited(action = "SUBMIT_FOR_APPROVAL", entityType = "Quotation")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUBMITTED", quotationService.submitForApproval(id)));
    }

    @Audited(action = "APPROVE", entityType = "Quotation")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("APPROVED", quotationService.approve(id)));
    }

    @Audited(action = "REJECT", entityType = "Quotation")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<QuotationResponseDTO>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("REJECTED", quotationService.reject(id)));
    }

    // Converting an APPROVED quotation into a real SalesOrder is the actual "commitment"
    // moment (§18.6: "Quotations may be converted into Sales Orders") — see Phase 3.
    @Audited(action = "CONVERT_TO_SALES_ORDER", entityType = "Quotation")
    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<SalesOrderResponseDTO>> convert(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("CONVERTED", quotationService.convertToSalesOrder(id)));
    }
}
