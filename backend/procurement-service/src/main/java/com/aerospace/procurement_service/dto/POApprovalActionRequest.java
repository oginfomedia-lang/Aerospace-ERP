package com.aerospace.procurement_service.dto;

import lombok.Data;

@Data
public class POApprovalActionRequest {
    // Mandatory on reject (PUR-BR-002); optional on approve
    private String remarks;
}
