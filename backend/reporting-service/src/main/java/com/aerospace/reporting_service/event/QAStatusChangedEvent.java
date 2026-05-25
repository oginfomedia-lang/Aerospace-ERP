package com.aerospace.reporting_service.event;

import lombok.Data;

@Data
public class QAStatusChangedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private String newStatus;
    private String decision;
    private String inspectorName;
    private String Location;
    private String remarks;
}
