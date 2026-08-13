package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.QuotationRequestDTO;
import com.aerospace.sales_service.dto.QuotationResponseDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;

import java.util.List;

public interface QuotationService {
    QuotationResponseDTO createQuotation(QuotationRequestDTO dto, String createdBy);
    QuotationResponseDTO updateQuotation(Long id, QuotationRequestDTO dto);
    List<QuotationResponseDTO> listQuotations(String status);
    QuotationResponseDTO getQuotationById(Long id);

    // Single-step status flips (DRAFT -> SUBMITTED -> APPROVED/REJECTED) — NOT wired to
    // common-lib's ApprovalEngine. That's deliberately reserved for Sales Orders (the
    // real commitment), since a quotation is cheap and reversible.
    QuotationResponseDTO submitForApproval(Long id);
    QuotationResponseDTO approve(Long id);
    QuotationResponseDTO reject(Long id);

    // The actual "commitment" moment (§18.6: "Quotations may be converted into Sales
    // Orders") — only an APPROVED quotation can convert; see QuotationServiceImpl.
    SalesOrderResponseDTO convertToSalesOrder(Long quotationId);
}