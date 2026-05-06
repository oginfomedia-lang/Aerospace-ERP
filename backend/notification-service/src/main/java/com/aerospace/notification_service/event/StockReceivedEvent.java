package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by Inventory Service when a new GRN (Goods Receipt Note) is processed.
 * The batch will be PENDING_QA until the QA service inspects it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockReceivedEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityReceived;
    private String supplier;
    private String location;
    private String expiryDate;          // ISO date string
    private String receivedBy;
    private LocalDateTime receivedAt;
}