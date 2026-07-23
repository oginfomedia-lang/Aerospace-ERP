package com.aerospace.procurement_service.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RfqCreatedEvent {
    private Long rfqId;
    private String rfqNumber;
    private String partName;
    private Integer quantity;
    private LocalDateTime dueDate;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
}
