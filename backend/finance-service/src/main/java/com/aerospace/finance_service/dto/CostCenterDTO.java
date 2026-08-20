package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostCenterDTO {
    private Long id;

    @NotBlank(message = "Cost center code is required")
    private String costCenterCode;

    @NotBlank(message = "Cost center name is required")
    private String costCenterName;

    private String description;
    private Long plantId;
    private Boolean activeStatus;
}
