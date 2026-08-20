package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.SupplierPayment.PaymentMethod;
import com.aerospace.finance_service.entity.SupplierPayment.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPaymentDTO {
    private Long id;
    private String paymentNumber;
    private String invoiceNumber;
    private String supplierRef;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String journalEntryRef;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdAt;
}
