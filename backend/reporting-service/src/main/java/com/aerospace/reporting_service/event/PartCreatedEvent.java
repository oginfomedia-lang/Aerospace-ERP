package com.aerospace.reporting_service.event;

import lombok.Data;

@Data
public class PartCreatedEvent {
    private Long partId;
    private String partName;
    private String skuCode;
    private String partType;
    private String criticality;
    private Integer reorderLevel;
    private String uom;
}
