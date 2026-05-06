package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by Inventory Service when stock is issued to production.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialIssuedEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityIssued;
    private Integer remainingQuantity;
    private String issuedTo;            // production order / department
    private String issuedBy;
    private String location;
    private LocalDateTime issuedAt;
}