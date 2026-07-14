package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.InvoiceActionRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceResponse;

import java.util.List;

public interface VendorInvoiceService {
    VendorInvoiceResponse createAndMatch(VendorInvoiceRequest request);
    List<VendorInvoiceResponse> getAll();
    VendorInvoiceResponse getById(Long id);
    VendorInvoiceResponse approve(Long id, InvoiceActionRequest request, String approverUsername);
    VendorInvoiceResponse reject(Long id, InvoiceActionRequest request, String approverUsername);
    VendorInvoiceResponse markPaid(Long id);
}
