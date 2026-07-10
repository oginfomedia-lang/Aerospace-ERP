package com.aerospace.inventory_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One batch/status bucket for a part (e.g. "40 units, batch B-102, PENDING_QA").
// A single part can have several of these at once — this is the authoritative,
// non-lossy view; PartResponseDTO's top-level status/quantity/batchNumber are
// just an APPROVED-stock summary derived from this list.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceDTO {
    private String batchNumber;
    private String status;
    private Integer quantity;
    private String location;
    private LocalDateTime expiryDate;
}
