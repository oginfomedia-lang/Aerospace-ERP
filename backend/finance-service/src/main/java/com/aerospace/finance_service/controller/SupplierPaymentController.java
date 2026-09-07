package com.aerospace.finance_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.finance_service.common.ApiResponse;
import com.aerospace.finance_service.dto.SupplierPaymentCreateDTO;
import com.aerospace.finance_service.dto.SupplierPaymentDTO;
import com.aerospace.finance_service.service.SupplierPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/ap/payments")
@RequiredArgsConstructor
public class SupplierPaymentController {

    private final SupplierPaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<List<SupplierPaymentDTO>> getAll() {
        return new ApiResponse<>("Supplier payments fetched", paymentService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    public ApiResponse<SupplierPaymentDTO> getById(@PathVariable Long id) {
        return new ApiResponse<>("Supplier payment fetched", paymentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "CREATE", entityType = "SupplierPayment")
    public ResponseEntity<ApiResponse<SupplierPaymentDTO>> create(
            @Valid @RequestBody SupplierPaymentCreateDTO dto, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Supplier payment recorded and posted",
                        paymentService.create(dto, auth.getName())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER','ADMIN','MANAGER')")
    @Audited(action = "CANCEL", entityType = "SupplierPayment")
    public ApiResponse<SupplierPaymentDTO> cancel(@PathVariable Long id, Authentication auth) {
        return new ApiResponse<>("Supplier payment cancelled",
                paymentService.cancel(id, auth.getName()));
    }
}
