package com.aerospace.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAStatusUpdatedEvent {
    private Long partId;
    private String batchNumber;
    private String status;
    private Integer approvedQty;
    private Integer rejectedQty;
    private String criteria;
    private String location;
    private String remarks;
    private String certificateNumber;
    private String inspectorName;
}
