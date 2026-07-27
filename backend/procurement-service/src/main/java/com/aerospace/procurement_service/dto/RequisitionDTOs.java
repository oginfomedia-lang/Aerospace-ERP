package com.aerospace.procurement_service.dto;

import com.aerospace.procurement_service.entity.PRSourceType;
import com.aerospace.procurement_service.entity.PRStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

public class RequisitionDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseRequisitionResponse {
        private Long id;
        private String prNumber;
        private Long partId;
        private String partName;
        private String skuCode;
        private Integer quantityNeeded;
        private LocalDateTime requiredByDate;
        private PRSourceType sourceType;
        private Long sourceWorkOrderId;
        private String sourceWoNumber;
        private PRStatus status;
        private String remarks;
        private LocalDateTime createdAt;

        // Populated from the most recently created RFQ for this PR (if any) — lets the UI
        // link straight from a PR row to its RFQ instead of a separate lookup on /rfqs.
        private Long latestRfqId;
        private String latestRfqNumber;
    }

    @Data
    public static class CreatePurchaseRequisitionRequest {
        @NotNull private Long partId;
        private String partName;
        private String skuCode;
        @NotNull private Integer quantityNeeded;
        private LocalDateTime requiredByDate;
        private String remarks;
    }
}