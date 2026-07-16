package com.aerospace.procurement_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalMatrixRequest {
    @NotBlank(message = "Module code is required")
    private String moduleCode;

    @NotNull(message = "Amount from is required")
    private BigDecimal amountFrom;

    @NotNull(message = "Amount to is required")
    private BigDecimal amountTo;

    @NotBlank(message = "Approver role is required")
    private String approverRole;

    @NotNull(message = "Level sequence is required")
    private Integer levelSequence;

    private Boolean isActive;
}
