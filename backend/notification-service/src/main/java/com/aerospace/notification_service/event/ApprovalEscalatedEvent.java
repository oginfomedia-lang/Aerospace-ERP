package com.aerospace.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalEscalatedEvent {
    private String moduleCode;
    private Long documentId;
    private int approvalLevel;
    private String newApproverRole;
    private LocalDateTime pendingSince;
}
