package com.aerospace.finance_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Local mirror of procurement-service's SupplierUpdatedEvent — field names must match
// exactly for Jackson to deserialize it off the wire (same convention as
// SupplierRegisteredEvent / VendorInvoiceApprovedEvent in this same package).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierUpdatedEvent {
    private long supplierId;
    private String supplierName;
    private String vendorCode;
    private boolean active;
    private String updatedBy;
}
