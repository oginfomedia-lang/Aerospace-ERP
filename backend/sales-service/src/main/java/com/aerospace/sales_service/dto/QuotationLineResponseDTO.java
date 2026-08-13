package com.aerospace.sales_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QuotationLineResponseDTO {
    private Long id;
    private Long partId;
    private String partNumber;
    private String partName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal taxPercent;
    private BigDecimal lineTotal;
}
