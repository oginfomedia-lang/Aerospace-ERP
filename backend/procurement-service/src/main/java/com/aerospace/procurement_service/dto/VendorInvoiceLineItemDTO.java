package com.aerospace.procurement_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoiceLineItemDTO {
    @NotBlank(message = "SKU Code is required")
    private String skuCode;

    @NotNull(message = "Invoiced quantity is required")
    @Min(value = 1, message = "Invoiced quantity must be at least 1")
    private Integer invoicedQuantity;

    @NotNull(message = "Invoiced unit price is required")
    private BigDecimal invoicedUnitPrice;

    private BigDecimal invoicedTotal;

    // Populated by the match — read-only in the response, ignored on request.
    private Integer matchedPoQuantity;
    private BigDecimal matchedPoUnitPrice;
    private Integer quantityVariance;
    private BigDecimal priceVariancePercent;
    private String lineStatus;
}
