package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.PartBasicDTO;
import com.aerospace.inventory_service.dto.PartSupplierRequest;
import com.aerospace.inventory_service.dto.PartSupplierResponse;

import java.util.List;

public interface PartSupplierService {
    List<PartSupplierResponse> getApprovedSuppliers(Long partId);
    PartSupplierResponse addApprovedSupplier(Long partId, PartSupplierRequest request);
    void removeApprovedSupplier(Long partId, Long supplierId);
    List<PartBasicDTO> getPartsForSupplier(Long supplierId);
}
