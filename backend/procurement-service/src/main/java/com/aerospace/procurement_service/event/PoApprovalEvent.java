package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoApprovalEvent {
    private String poNumber;
    private String supplierName;
    private BigDecimal totalAmount;
    private String currency;
    private String status; // PENDING_APPROVAL, APPROVED
    private String approverRole; // role required at the pending level; null once fully approved
}
