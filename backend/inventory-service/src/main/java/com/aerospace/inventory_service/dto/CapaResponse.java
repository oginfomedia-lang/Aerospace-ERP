package com.aerospace.inventory_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CapaResponse {
    private Long id;
    private String capaNumber;
    private Long ncrId;
    private String ncrNumber;
    private String ncrPartName;
    private String ncrBatchNumber;
    private String capaType;
    private String status;
    private String rootCause;
    private String correctiveAction;
    private String preventiveAction;
    private LocalDateTime targetCloseDate;
    private String ownerName;
    private String verificationRemarks;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime closedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<CapaHistoryDTO> history;
}
