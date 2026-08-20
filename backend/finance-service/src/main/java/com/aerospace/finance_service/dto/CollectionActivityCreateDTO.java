package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.CollectionActivity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollectionActivityCreateDTO {
    @NotBlank private String customerRef;
    private String invoiceRef;
    @NotNull private LocalDate activityDate;
    @NotNull private ActivityType activityType;
    private String notes;
    private LocalDate promisedPaymentDate;
    private BigDecimal promisedAmount;
    private LocalDate followUpDate;
}
