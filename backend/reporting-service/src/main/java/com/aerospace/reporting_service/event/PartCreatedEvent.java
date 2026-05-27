package com.aerospace.reporting_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartCreatedEvent {
    private Long partId;
    private String partName;
    private String skuCode;
    private String partType;
    private String criticality;
    private Integer reorderLevel;
    private String uom;
}
