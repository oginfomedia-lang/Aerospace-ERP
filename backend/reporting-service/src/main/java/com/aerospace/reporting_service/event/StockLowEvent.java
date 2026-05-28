package com.aerospace.reporting_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockLowEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private int currentQuantity;
    private int reorderLevel;
    private String location;
}
