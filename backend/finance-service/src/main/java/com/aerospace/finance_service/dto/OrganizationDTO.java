package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationDTO {
    private Long id;

    @NotBlank(message = "Organization code is required")
    private String orgCode;

    @NotBlank(message = "Organization name is required")
    private String orgName;

    private String baseCurrency;
    private Boolean activeStatus;
}
