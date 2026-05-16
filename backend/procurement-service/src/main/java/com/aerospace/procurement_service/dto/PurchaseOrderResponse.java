package com.aerospace.procurement_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private String supplierName;
    private String status;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private Double totalAmount;
    private String currency;
    private List<POLineItemDTO> items;
    private String remarks;
}
