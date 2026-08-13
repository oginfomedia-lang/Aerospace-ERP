package com.aerospace.sales_service.dto;

import com.aerospace.sales_service.entity.SalesOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SalesOrderResponseDTO {
    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName; // denormalized, resolved server-side from SalesOrder.customer
    private String orderType;
    private LocalDate requestedDeliveryDate;
    private LocalDate promisedDeliveryDate;
    private String shippingAddress;
    private String priority;
    private SalesOrderStatus status;
    private BigDecimal totalAmount;
    private Long sourceQuotationId;
    private Integer revisionNumber;
    private Long supersedesId;
    private Boolean isCurrentRevision;
    private List<SalesOrderLineResponseDTO> lines;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}