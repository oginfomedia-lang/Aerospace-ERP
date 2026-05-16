package com.aerospace.procurement_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POLineItemDTO {
    @NotBlank(message="SKU Code is required")
    private String skuCode;

    @NotNull(message="Quantity is required")
    @Min(value=1, message="Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message="Unit price is required")
    private Double unitPrice;

    private Double totalPrice;

    private String uom;

}
