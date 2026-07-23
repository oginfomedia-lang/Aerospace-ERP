package com.aerospace.auth_service.event;
import lombok.*;

//published so notification-service(not auth) sends the activation email - 
//keeps this repo's "no cross-service REST" rule intact.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInviteCreatedEvent {
    private Long supplierId;
    private String supplierName;
    private String email;
    private String activationToken;
    private String activationUrl;
}
