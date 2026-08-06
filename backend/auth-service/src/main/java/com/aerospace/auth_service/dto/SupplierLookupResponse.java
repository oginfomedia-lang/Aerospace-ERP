package com.aerospace.auth_service.dto;

import lombok.Data;

// Minimal shape we need from procurement-service's Supplier JSON — just enough
// to check the confirmation gate, without duplicating the whole vendor record.
@Data
public class SupplierLookupResponse {
    private Long id;
    private String confirmationStatus;
}
