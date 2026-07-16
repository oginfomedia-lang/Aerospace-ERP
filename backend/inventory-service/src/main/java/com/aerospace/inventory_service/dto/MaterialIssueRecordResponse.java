package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialIssueRecordResponse {
    private Long id;
    private String issueNumber;
    private Long partId;
    private String partName;
    private Integer totalQuantityIssued;
    private Long workOrderId;
    private String woNumber;
    private Long lineItemId;
    private String issuedTo;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private List<MaterialIssueRecordLineResponse> lines;
}
