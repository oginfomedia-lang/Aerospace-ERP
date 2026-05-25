package com.aerospace.reporting_service.event;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MaterialIssuedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityIssued;
    private Integer remainingQuantity;
    private String issuedTo;
    private String issuedBy;
    private String location;
    private LocalDateTime issuedAt;
}
