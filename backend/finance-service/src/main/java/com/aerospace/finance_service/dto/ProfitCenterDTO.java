package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfitCenterDTO {
    private Long id;

    @NotBlank(message = "Profit center code is required")
    private String profitCenterCode;

    @NotBlank(message = "Profit center name is required")
    private String profitCenterName;

    private Long plantId;
    private Boolean activeStatus;
}
