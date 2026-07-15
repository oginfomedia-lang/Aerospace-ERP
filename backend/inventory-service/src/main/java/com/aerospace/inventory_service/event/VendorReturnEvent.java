package com.aerospace.inventory_service.event;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorReturnEvent {
    private Long partId;
    private String skuCode;
    private String partName;
    private String batchNumber;
    private Integer quantity;
    private String ncrNumber;
    private String poNumber;
    private String dispositionedBy;
    private String remarks;
    private LocalDateTime dispositionedAt;
}
