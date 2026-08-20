package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.SupplierPayment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPaymentCreateDTO {
    @NotBlank private String invoiceNumber;

    // Real FK — replaces the old free-text supplierRef as the actual input. Must
    // reference a supplier already synced into finance-service's local
    // SupplierReference mirror (see SupplierEventConsumer); a not-yet-synced id
    // gets a 404 explaining the possible Kafka lag, not a generic "not found".
    @NotNull private Long supplierId;

    @NotNull  private LocalDate paymentDate;
    @NotNull @Positive private BigDecimal amount;
    @NotBlank private String currency;
    @NotNull  private PaymentMethod paymentMethod;
    private String remarks;
    private Long costCenterId;
}
