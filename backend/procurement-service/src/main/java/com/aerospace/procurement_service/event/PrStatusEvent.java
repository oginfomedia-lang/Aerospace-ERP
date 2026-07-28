package com.aerospace.procurement_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrStatusEvent {
    private String prNumber;
    private String partName;
    private Integer quantityNeeded;
    private String status; // PENDING_APPROVAL, APPROVED, REJECTED
    private String actor;
    private String remarks;
}
