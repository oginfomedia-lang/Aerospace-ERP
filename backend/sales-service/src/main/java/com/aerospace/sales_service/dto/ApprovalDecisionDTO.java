package com.aerospace.sales_service.dto;

import lombok.Data;

// Body for /approve and /reject. Deliberately just remarks — the approver's own
// identity and roles come from the authenticated request (Authentication auth in the
// controller), never from this body, or anyone could approve pretending to be someone else.
@Data
public class ApprovalDecisionDTO {
    private String remarks;
}
