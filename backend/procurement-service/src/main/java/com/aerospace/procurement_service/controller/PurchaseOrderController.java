package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.PurchaseOrderRequest;
import com.aerospace.procurement_service.dto.PurchaseOrderResponse;
import com.aerospace.procurement_service.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse created = poService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping
    public ResponseEntity<java.util.List<PurchaseOrderResponse>> getAllPurchaseOrders() {
        return ResponseEntity.ok(poService.getAllPurchaseOrders());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponse> updatePOStatus(
            @PathVariable Long id, 
            @RequestParam com.aerospace.procurement_service.entity.POStatus status) {
        return ResponseEntity.ok(poService.updatePOStatus(id, status));
    }
}
