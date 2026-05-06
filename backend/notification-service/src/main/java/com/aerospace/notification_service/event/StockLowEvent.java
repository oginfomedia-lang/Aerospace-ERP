package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by Inventory Service when a part's quantity drops below reorderLevel.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockLowEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer currentQuantity;
    private Integer reorderLevel;
    private String location;
    private LocalDateTime triggeredAt;
}