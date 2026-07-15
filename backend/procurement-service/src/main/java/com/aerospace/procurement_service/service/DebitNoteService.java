package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.DebitNoteResponse;
import com.aerospace.procurement_service.event.VendorReturnEvent;

import java.util.List;

public interface DebitNoteService {
    void createFromVendorReturn(VendorReturnEvent event);
    List<DebitNoteResponse> getAll();
    DebitNoteResponse markSettled(Long id);
}
