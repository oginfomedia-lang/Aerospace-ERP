// SupplierService.java
package com.aerospace.procurement_service.service;

import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import java.util.List;

public interface SupplierService {
    Supplier createSupplier(SupplierRequest request);
    Supplier getSupplierById(Long id);
    List<Supplier> getAllSuppliers();
    Supplier updateSupplier(Long id, SupplierRequest request);
    Supplier setActive(Long id, boolean active);
}
