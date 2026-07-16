package com.aerospace.production_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteStageRequestDTO {
    @NotNull
    private Integer quantityGood;
    @NotNull
    private Integer quantityRework;
    @NotNull
    private Integer quantityScrap;
    private String inspectedBy;
    private String remarks;
}
