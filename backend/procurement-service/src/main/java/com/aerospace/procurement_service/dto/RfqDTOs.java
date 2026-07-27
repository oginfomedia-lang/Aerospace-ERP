package com.aerospace.procurement_service.dto;

import com.aerospace.procurement_service.entity.QuotationStatus;
import com.aerospace.procurement_service.entity.RfqStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RfqDTOs {

    @Data
    public static class CreateRfqRequest {
        @NotEmpty private List<Long> supplierIds;
        @NotNull private Integer quantity;
        private String specification;
        @NotNull private LocalDateTime dueDate;
    }

    @Data
    public static class InviteMoreSuppliersRequest {
        @NotEmpty private List<Long> supplierIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqResponse {
        private Long id;
        private String rfqNumber;
        private Long purchaseRequisitionId;
        private String partName;
        private String skuCode;
        private List<InvitedSupplierSummary> invitedSuppliers;
        private Integer quantity;
        private String specification;
        private LocalDateTime dueDate;
        private RfqStatus status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitedSupplierSummary {
        private Long supplierId;
        private String supplierName;
        private boolean hasQuoted;
    }

    @Data
    public static class SubmitQuotationRequest {
        @NotNull private BigDecimal unitPrice;
        @NotNull private Integer leadTimeDays;
        private LocalDate validityDate;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotationResponse {
        private Long id;
        private Long rfqId;
        private String rfqNumber;
        private String partName;
        private Long supplierId;
        private String supplierName;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private Integer leadTimeDays;
        private LocalDate validityDate;
        private String remarks;
        private QuotationStatus status;
        private LocalDateTime submittedAt;
    }
}