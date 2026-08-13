package com.aerospace.sales_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.CustomerRequestDTO;
import com.aerospace.sales_service.dto.CustomerResponseDTO;
import com.aerospace.sales_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sales/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES_EXECUTIVE','SALES_MANAGER','MANAGER','ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    @Audited(action = "CREATE", entityType = "Customer")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> register(@Valid @RequestBody CustomerRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("CUSTOMER_REGISTERED", customerService.registerCustomer(dto)));
    }

    @Audited(action = "UPDATE", entityType = "Customer")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> update(
            @PathVariable Long id, @Valid @RequestBody CustomerRequestDTO dto) {
        return ResponseEntity.ok(new ApiResponse<>("CUSTOMER_UPDATED", customerService.updateCustomer(id, dto)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", customerService.listCustomers(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", customerService.getCustomerById(id)));
    }
}