package com.aerospace.sales_service.dto;

import com.aerospace.sales_service.entity.QuotationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuotationResponseDTO {
    private Long id;
    private String quotationNumber;
    private Long customerId;
    private String customerName; // denormalized, resolved server-side from Quotation.customer
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String currency;
    private String paymentTerms;
    private String deliveryTerms;
    private BigDecimal totalAmount;
    private QuotationStatus status;
    private Long convertedToSalesOrderId;
    private List<QuotationLineResponseDTO> lines;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
