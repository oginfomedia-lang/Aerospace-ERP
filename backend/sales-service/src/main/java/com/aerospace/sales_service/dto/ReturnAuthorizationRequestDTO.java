package com.aerospace.sales_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ReturnAuthorizationRequestDTO {
    @NotNull(message = "Sales order is required")
    private Long salesOrderId;

    @NotNull(message = "Part is required")
    private Long partId;
    private String partNumber;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    private String reasonCode;
    private String reasonDescription;
}
