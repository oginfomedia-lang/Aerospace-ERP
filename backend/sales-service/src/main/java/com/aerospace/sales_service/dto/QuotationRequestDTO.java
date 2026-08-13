package com.aerospace.sales_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationRequestDTO {
    @NotNull(message = "Customer is required")
    private Long customerId;

    private LocalDate validFrom;
    private LocalDate validUntil;

    private String currency;
    private String paymentTerms;
    private String deliveryTerms;

    @NotEmpty(message = "A quotation needs at least one line item")
    @Valid
    private List<QuotationLineRequestDTO> lines;
}
