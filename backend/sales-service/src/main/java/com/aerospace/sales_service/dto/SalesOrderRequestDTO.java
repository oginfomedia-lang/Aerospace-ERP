package com.aerospace.sales_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderRequestDTO {
    @NotNull(message = "Customer is required")
    private Long customerId;

    private String orderType;
    private LocalDate requestedDeliveryDate;
    private String shippingAddress;
    private String priority;

    // promisedDeliveryDate is deliberately NOT here — only the ATP feature (later) is
    // allowed to set it; a user can request a date, never promise one directly.

    @NotEmpty(message = "A sales order needs at least one line item")
    @Valid
    private List<SalesOrderLineRequestDTO> lines;
}