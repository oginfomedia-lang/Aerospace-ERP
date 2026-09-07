package com.aerospace.common.approval;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalMatrixRequest {
    @NotBlank
    private String moduleCode;

    @NotNull @PositiveOrZero
    private BigDecimal amountFrom;

    // Nullable on purpose — a null amountTo means "and above" (the top, open-ended band).
    @PositiveOrZero
    private BigDecimal amountTo;

    @NotBlank
    private String approverRole;
    private int levelSequence;
    private Boolean isActive;
    private Integer escalationHours;
    private String escalationToRole;
    private String approverUsername;

    @AssertTrue(message = "amountTo must be greater than amountFrom when both are set")
    private boolean isRangeValid() {
        return amountTo == null || amountFrom == null || amountTo.compareTo(amountFrom) > 0;
    }
}