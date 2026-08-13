package com.aerospace.sales_service.dto;

import com.aerospace.sales_service.entity.ReturnDisposition;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReturnAuthorizationResponseDTO {
    private Long id;
    private String rmaNumber;
    private Long salesOrderId;
    private String orderNumber; // denormalized, resolved server-side from ReturnAuthorization.salesOrder
    private Long partId;
    private String partNumber;
    private Integer quantity;
    private String reasonCode;
    private String reasonDescription;
    private ReturnDisposition disposition;
    private String receivedBatchNumber;
    private String inspectedBy;
    private LocalDateTime inspectedAt;
    private String inspectionNotes;
    private String requestedBy;
    private LocalDateTime createdAt;
}
