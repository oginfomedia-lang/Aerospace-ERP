package com.aerospace.sales_service.client.dto;

import lombok.Data;
import java.time.LocalDateTime;

// Local mirror of inventory-service's StockBalanceDTO — one batch/status bucket for a
// part. Only APPROVED-status rows are usable for allocation.
@Data
public class StockBalanceInfo {
    private String batchNumber;
    private String status;
    private Integer quantity;
    private String location;
    private LocalDateTime expiryDate;
}
