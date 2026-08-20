package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxJurisdictionDTO {
    private Long id;

    @NotBlank(message = "Jurisdiction code is required")
    private String jurisdictionCode;

    @NotBlank(message = "Jurisdiction name is required")
    private String jurisdictionName;

    private Boolean activeStatus;
}
