package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.RfqDTOs.*;
import com.aerospace.procurement_service.service.RfqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RfqController {

    private final RfqService rfqService;

    // ── Internal (Procurement Officer / Manager) ────────────────────────────

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @PostMapping("/api/purchase-requisitions/{prId}/create-rfq")
    public ResponseEntity<RfqResponse> createRfq(@PathVariable Long prId,
                                                  @Valid @RequestBody CreateRfqRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.createRfq(prId, request));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @GetMapping("/api/rfqs")
    public ResponseEntity<List<RfqResponse>> getAll() {
        return ResponseEntity.ok(rfqService.getAll());
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @GetMapping("/api/rfqs/{id}")
    public ResponseEntity<RfqResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(rfqService.getById(id));
    }

    @PreAuthorize("hasAnyRole('PROCUREMENT_OFFICER','MANAGER','ADMIN')")
    @GetMapping("/api/rfqs/{id}/quotations")
    public ResponseEntity<List<QuotationResponse>> getQuotations(@PathVariable Long id) {
        return ResponseEntity.ok(rfqService.getQuotations(id));
    }

    // Award is the "top management approves the best quotation" business rule —
    // deliberately a step above PROCUREMENT_OFFICER.
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping("/api/rfqs/{id}/select-quotation/{quotationId}")
    public ResponseEntity<Void> selectWinner(@PathVariable Long id, @PathVariable Long quotationId) {
        rfqService.selectWinner(id, quotationId);
        return ResponseEntity.ok().build();
    }

    // ── Supplier portal ──────────────────────────────────────────────────────
    // supplierId is a request attribute set by JwtAuthenticationFilter from the
    // token's own "supplierId" claim — never trust a client-supplied supplier ID here.

    @PreAuthorize("hasRole('SUPPLIER')")
    @GetMapping("/api/supplier-portal/rfqs")
    public ResponseEntity<List<RfqResponse>> getMyOpenRfqs(@RequestAttribute("supplierId") Long supplierId) {
        return ResponseEntity.ok(rfqService.getOpenRfqsForSupplier(supplierId));
    }

    @PreAuthorize("hasRole('SUPPLIER')")
    @GetMapping("/api/supplier-portal/rfqs/{id}")
    public ResponseEntity<RfqResponse> getMyRfq(@PathVariable Long id,
                                                 @RequestAttribute("supplierId") Long supplierId) {
        return ResponseEntity.ok(rfqService.getRfqForSupplier(id, supplierId));
    }

    @PreAuthorize("hasRole('SUPPLIER')")
    @PostMapping("/api/supplier-portal/rfqs/{id}/quote")
    public ResponseEntity<QuotationResponse> submitQuote(@PathVariable Long id,
                                                          @RequestAttribute("supplierId") Long supplierId,
                                                          @Valid @RequestBody SubmitQuotationRequest request) {
        return ResponseEntity.ok(rfqService.submitQuotation(id, supplierId, request));
    }

    @PreAuthorize("hasRole('SUPPLIER')")
    @GetMapping("/api/supplier-portal/quotations")
    public ResponseEntity<List<QuotationResponse>> getMyQuotations(@RequestAttribute("supplierId") Long supplierId) {
        return ResponseEntity.ok(rfqService.getMyQuotations(supplierId));
    }
}