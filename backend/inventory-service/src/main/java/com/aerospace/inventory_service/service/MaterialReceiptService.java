package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.FormGrnRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptResponse;

import java.util.List;

public interface MaterialReceiptService {
    // Physical gate log only — no stock/GRN side effects yet.
    MaterialReceiptResponse createReceipt(MaterialReceiptRequest request);
    List<MaterialReceiptResponse> getAll(String status);
    MaterialReceiptResponse getById(Long id);

    // Forms the GRN(s) from an existing, not-yet-processed receipt — this is what
    // actually posts stock to PENDING_QA/APPROVED via PartService.addStock.
    MaterialReceiptResponse formGrn(Long id, FormGrnRequest request);
}
