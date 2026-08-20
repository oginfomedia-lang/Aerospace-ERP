package com.aerospace.procurement_service.event;
import lombok.*;

// Published whenever a supplier's record changes — a plain edit (updateSupplier) or
// an active/inactive toggle (setActive) both funnel into this one event, same as
// confirmSupplier/rejectSupplier already share SupplierConfirmationDecidedEvent.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupplierUpdatedEvent {
    private long supplierId;
    private String supplierName;
    private String vendorCode;
    private boolean active;
    private String updatedBy;
}
