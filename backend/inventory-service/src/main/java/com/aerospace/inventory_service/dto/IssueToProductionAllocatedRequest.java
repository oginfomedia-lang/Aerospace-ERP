package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Explicit batch-split issuance — the caller (a picker form showing real batch
// availability) decides exactly which batches and how much from each, instead of
// the server auto-computing a FIFO split.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueToProductionAllocatedRequest {
    private List<BatchAllocationRequest> allocations;
    private Long workOrderId;
    private String woNumber;
    private Long lineItemId;
    private String issuedTo;
    private String issuedBy;
}
