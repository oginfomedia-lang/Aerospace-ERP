package com.aerospace.notification_service.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class RfqCreatedEvent {
    private long rfqId;
    private String rfqNumber;
    private String partName;
    private Integer quantity;
    private LocalDateTime dueDate;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;
}
