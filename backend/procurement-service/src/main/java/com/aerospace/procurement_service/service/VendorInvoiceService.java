package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.InvoiceActionRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceResponse;

import java.util.List;
import java.util.Set;

public interface VendorInvoiceService {
    VendorInvoiceResponse createAndMatch(VendorInvoiceRequest request);
    List<VendorInvoiceResponse> getAll();
    VendorInvoiceResponse getById(Long id);
    VendorInvoiceResponse approve(Long id, InvoiceActionRequest request, String approverUsername, Set<String> approverRoles);
    VendorInvoiceResponse reject(Long id, InvoiceActionRequest request, String approverUsername, Set<String> approverRoles);
    VendorInvoiceResponse markPaid(Long id);
}
