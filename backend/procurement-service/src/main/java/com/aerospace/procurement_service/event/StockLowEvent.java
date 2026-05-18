package com.aerospace.procurement_service.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLowEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private int currentQuantity;
    private int reorderLevel;
    private String location;
    
}
