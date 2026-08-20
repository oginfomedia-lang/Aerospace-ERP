package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.CollectionActivity.ActivityStatus;
import com.aerospace.finance_service.entity.CollectionActivity.ActivityType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollectionActivityDTO {
    private Long id;
    private String activityNumber;
    private String customerRef;
    private String invoiceRef;
    private LocalDate activityDate;
    private ActivityType activityType;
    private String notes;
    private LocalDate promisedPaymentDate;
    private BigDecimal promisedAmount;
    private LocalDate followUpDate;
    private ActivityStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
}
