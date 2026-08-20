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
    @NotBlank private String supplierRef;
    @NotNull  private LocalDate paymentDate;
    @NotNull @Positive private BigDecimal amount;
    @NotBlank private String currency;
    @NotNull  private PaymentMethod paymentMethod;
    private String remarks;
    private Long costCenterId;
}
