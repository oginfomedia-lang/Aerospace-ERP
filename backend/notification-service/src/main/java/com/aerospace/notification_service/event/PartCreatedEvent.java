package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by Inventory Service when a new part is registered in the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartCreatedEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private String category;
    private Integer initialQuantity;
    private Integer reorderLevel;
    private String location;
    private String createdBy;
    private LocalDateTime createdAt;
}