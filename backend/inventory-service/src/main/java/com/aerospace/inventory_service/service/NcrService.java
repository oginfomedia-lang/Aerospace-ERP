package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.DispositionRequest;
import com.aerospace.inventory_service.dto.NcrResponse;

import java.util.List;

public interface NcrService {
    List<NcrResponse> getAll(String status);
    NcrResponse getById(Long id);

    // RETURN_TO_VENDOR/SCRAP remove the REJECTED quantity from the ledger entirely;
    // USE_AS_IS moves it into APPROVED under the disposition's special approval.
    NcrResponse disposition(Long id, DispositionRequest request);
}
