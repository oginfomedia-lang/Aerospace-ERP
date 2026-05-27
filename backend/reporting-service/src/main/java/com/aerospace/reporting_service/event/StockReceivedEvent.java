package com.aerospace.reporting_service.event;

import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockReceivedEvent {
    private Long partId;
    private String partName;
    private String batchNumber;
    private Integer quantityReceived;
    private String supplier;
    private String location;
    private String receivedBy;
    private LocalDateTime timestamp;
}
