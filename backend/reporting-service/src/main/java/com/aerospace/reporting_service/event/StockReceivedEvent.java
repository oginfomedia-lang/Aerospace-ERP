package com.aerospace.reporting_service.event;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
