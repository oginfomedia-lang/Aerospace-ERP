package com.aerospace.common.approval;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalMatrixRequest {
    private String moduleCode;
    private BigDecimal amountFrom;
    private BigDecimal amountTo;
    private String approverRole;
    private int levelSequence;
    private Boolean isActive;
    private Integer escalationHours;
    private String escalationToRole;
    private String approverUsername;
}