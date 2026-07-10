package com.aerospace.procurement_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class POApprovalHistoryDTO {
    private int approvalLevel;
    private String approverRole;
    private String status;
    private String approverUsername;
    private LocalDateTime approvedAt;
    private String remarks;
    private String digitalSignatureHash;
    private int documentVersion;
}
