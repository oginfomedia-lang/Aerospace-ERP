package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.PeriodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiscalYearDTO {
    private Long id;

    @NotBlank(message = "Fiscal year code is required")
    private String fiscalYearCode;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private PeriodStatus status;
}
