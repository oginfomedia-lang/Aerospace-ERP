package com.aerospace.procurement_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderRequest {
    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDateTime deliveryDate;
    private String currency;
    private String remarks;

    @NotEmpty(message = "Purchase order must have at least one item")
    private List<POLineItemDTO> items;
}
