package com.aerospace.sales_service.dto;

import com.aerospace.sales_service.entity.DispatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DispatchResponseDTO {
    private Long id;
    private String dispatchNumber;
    private Long salesOrderId;
    private String orderNumber; // denormalized, resolved server-side from Dispatch.salesOrder
    private DispatchStatus status;
    private String carrierName;
    private String trackingNumber;
    private String packedBy;
    private String dispatchedBy;
    private LocalDateTime packedAt;
    private LocalDateTime dispatchedAt;
    private List<DispatchLineResponseDTO> lines;
    private LocalDateTime createdAt;
}
