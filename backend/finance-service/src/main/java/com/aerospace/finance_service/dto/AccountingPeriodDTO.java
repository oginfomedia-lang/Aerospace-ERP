package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.PeriodStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountingPeriodDTO {
    private Long id;
    private String fiscalYearCode;
    private Integer periodNumber;
    private String periodLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodStatus status;
    private LocalDateTime closedAt;
    private String closedBy;
}
