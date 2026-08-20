package com.aerospace.finance_service.dto;

import lombok.*;

// Deliberately omits registeredBy/syncedAt — internal bookkeeping, not needed by a
// supplier picker dropdown.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierReferenceDTO {
    private Long id;
    private String supplierName;
    private String vendorCode;
}
