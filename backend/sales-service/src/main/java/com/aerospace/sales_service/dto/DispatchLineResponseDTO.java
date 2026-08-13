package com.aerospace.sales_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchLineResponseDTO {
    private Long id;
    private Long salesOrderLineId;
    private Long partId;
    private String partNumber;
    private Integer quantity;
}
