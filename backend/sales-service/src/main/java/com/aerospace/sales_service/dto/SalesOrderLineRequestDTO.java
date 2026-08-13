package com.aerospace.sales_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesOrderLineRequestDTO {
    private Long partId;
    private String partNumber;
    private String partName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    private BigDecimal discountPercent;
    private BigDecimal taxPercent;
}