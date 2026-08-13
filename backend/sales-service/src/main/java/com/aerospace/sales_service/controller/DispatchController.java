package com.aerospace.sales_service.controller;

import com.aerospace.common.audit.Audited;
import com.aerospace.sales_service.common.ApiResponse;
import com.aerospace.sales_service.dto.ConfirmDispatchRequestDTO;
import com.aerospace.sales_service.dto.DispatchResponseDTO;
import com.aerospace.sales_service.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// §18.10: Pick, Pack & Ship. Role-gated to warehouse execution roles, not the general
// SALES_EXECUTIVE set — creating an order and physically fulfilling it are different jobs.
@RestController
@RequestMapping("/api/v1/sales/dispatch")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE_OPERATOR','SALES_MANAGER','MANAGER','ADMIN')")
public class DispatchController {

    private final DispatchService dispatchService;

    @Audited(action = "CREATE_PICK_LIST", entityType = "Dispatch")
    @PostMapping
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> createPickList(@RequestParam Long orderId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("PICK_LIST_CREATED", dispatchService.createPickList(orderId)));
    }

    @Audited(action = "MARK_PACKED", entityType = "Dispatch")
    @PostMapping("/{id}/pack")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> markPacked(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(new ApiResponse<>("PACKED", dispatchService.markPacked(id, auth.getName())));
    }

    @Audited(action = "CONFIRM_DISPATCH", entityType = "Dispatch")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> confirmDispatch(
            @PathVariable Long id, @Valid @RequestBody ConfirmDispatchRequestDTO dto, Authentication auth) {
        return ResponseEntity.ok(new ApiResponse<>("DISPATCHED",
                dispatchService.confirmDispatch(id, dto.getCarrierName(), dto.getTrackingNumber(), auth.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DispatchResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", dispatchService.getById(id)));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<ApiResponse<List<DispatchResponseDTO>>> listByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", dispatchService.listByOrder(orderId)));
    }
}
