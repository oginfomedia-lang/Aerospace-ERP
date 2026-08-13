package com.aerospace.sales_service.service;

import com.aerospace.sales_service.dto.ReturnAuthorizationRequestDTO;
import com.aerospace.sales_service.dto.ReturnAuthorizationResponseDTO;
import com.aerospace.sales_service.entity.ReturnDisposition;

import java.util.List;

public interface ReturnAuthorizationService {
    // Approves the IDEA of a return before anything physically moves.
    ReturnAuthorizationResponseDTO authorizeReturn(ReturnAuthorizationRequestDTO dto, String requestedBy);

    // The physical goods actually arrive at the warehouse — received into inventory as
    // PENDING_QA (or straight to APPROVED if the part doesn't require QC), the same
    // "not trusted yet" gate any GRN receipt goes through.
    ReturnAuthorizationResponseDTO receiveReturn(Long rmaId, String location);

    // The real decision: accept (back to sellable stock), reject (scrapped),
    // repair/replace/credit-recommended (flags for a downstream workflow this pass
    // doesn't build).
    ReturnAuthorizationResponseDTO inspectAndDispose(Long rmaId, ReturnDisposition disposition, String inspectedBy, String notes);

    List<ReturnAuthorizationResponseDTO> listReturns(String disposition);
    ReturnAuthorizationResponseDTO getById(Long id);
}
