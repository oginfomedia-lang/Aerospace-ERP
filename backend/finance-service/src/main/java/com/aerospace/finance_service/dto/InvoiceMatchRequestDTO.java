package com.aerospace.finance_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceMatchRequestDTO {
    @NotBlank private String poNumber;
    @NotBlank private String grnNumber;
    @NotBlank private String invoiceNumber;
    @NotNull @Positive private BigDecimal poAmount;
    @NotNull @Positive private BigDecimal grnAmount;
    @NotNull @Positive private BigDecimal invoiceAmount;
}
