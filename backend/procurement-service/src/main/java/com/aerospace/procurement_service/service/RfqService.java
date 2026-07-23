package com.aerospace.procurement_service.service;

import java.util.List;

import com.aerospace.procurement_service.dto.RfqDTOs.CreateRfqRequest;
import com.aerospace.procurement_service.dto.RfqDTOs.QuotationResponse;
import com.aerospace.procurement_service.dto.RfqDTOs.RfqResponse;
import com.aerospace.procurement_service.dto.RfqDTOs.SubmitQuotationRequest;

public interface RfqService {
    RfqResponse createRfq(Long prId, CreateRfqRequest request);
    List<RfqResponse> getAll();
    RfqResponse getById(Long id);
    List<QuotationResponse> getQuotations(Long rfqId);
    void selectWinner(Long rfqId, Long quotationId);

    //supplier-portal side - every method here takes the supplierId straight from the
    //jwt claim, never from a client supplied param
    List<RfqResponse> getOpenRfqsForSupplier(Long supplerId);
    RfqResponse getRfqForSupplier(Long rfqId, Long supplierId);
    QuotationResponse submitQuotation(Long rfqId, Long supplierId, SubmitQuotationRequest request);
    List<QuotationResponse> getMyQuotations(Long supplierId);
}
