package com.aerospace.common.approval.report;

import lombok.*;

@Data @Builder
public class SlaComplianceDetailRow {
    String moduleCode; Long documentId; int documentVersion; int approvalLevel; String approverRole;
    Integer escalationHours; boolean escalated; boolean compliant;
}