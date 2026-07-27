package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.CapaOpenRequest;
import com.aerospace.inventory_service.dto.CapaResponse;
import com.aerospace.inventory_service.dto.CapaTransitionRequest;

import java.util.List;

public interface CapaService {
    CapaResponse openCapa(Long ncrId, CapaOpenRequest request);
    CapaResponse logRootCause(Long id, CapaTransitionRequest request);
    CapaResponse planActions(Long id, CapaTransitionRequest request);
    CapaResponse startImplementation(Long id, CapaTransitionRequest request);
    CapaResponse submitForVerification(Long id, CapaTransitionRequest request);
    CapaResponse verifyAndClose(Long id, CapaTransitionRequest request);
    CapaResponse reject(Long id, CapaTransitionRequest request);
    CapaResponse getById(Long id);
    List<CapaResponse> getByNcr(Long ncrId);
    List<CapaResponse> getAll(String status);
}
