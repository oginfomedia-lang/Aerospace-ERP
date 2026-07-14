package com.aerospace.procurement_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

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
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String uom;

    // Snapshotted from the Part at line-add time (frontend fills this from the
    // selected Part, read-only in the UI — not a per-PO negotiated value)
    private String hsnCode;
    private BigDecimal taxRatePercent;

    // QA outcome (read-only, populated from the PO line item once inspected)
    private Integer acceptedQuantity;
    private Integer rejectedQuantity;
    private String ncrNumber;

}
