package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alternative QA result event shape (some services emit this instead of QAStatusChangedEvent).
 * Both are handled by the consumer — whichever your QA service publishes will work.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QAResultEvent {

    private Long partId;
    private String partName;
    private String batchNumber;
    private String result;              // APPROVED | REJECTED
    private String remarks;
    private String inspectorName;
    private String location;
    private boolean expired;
    private String expiryDate;
    private LocalDateTime inspectedAt;
}