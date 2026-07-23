package com.aerospace.notification_service.event;

import lombok.*;

// Mirrors auth-service's event of the same name.
@Data @NoArgsConstructor @AllArgsConstructor
public class SupplierInviteCreatedEvent {
    private Long supplierId;
    private String supplierName;
    private String email;
    private String activationToken;
    private String activationUrl;
}