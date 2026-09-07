package com.aerospace.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MaterialReceiptItemRequest {
    @NotBlank
    private String skuCode;
    @NotNull
    @Positive
    private Integer quantity;
    private String uom;
}
