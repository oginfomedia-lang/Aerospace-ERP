package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlantDTO {
    private Long id;

    @NotBlank(message = "Plant code is required")
    private String plantCode;

    @NotBlank(message = "Plant name is required")
    private String plantName;

    private Long organizationId;
    private Boolean activeStatus;
}
