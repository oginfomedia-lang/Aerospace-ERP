package com.aerospace.reporting_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QAStatusChangedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private String newStatus;
    private String decision;
    private String inspectorName;
    private String location;
    private String remarks;
}
