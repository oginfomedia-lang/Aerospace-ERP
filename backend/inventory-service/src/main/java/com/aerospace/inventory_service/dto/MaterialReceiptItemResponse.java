package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialReceiptItemResponse {
    private Long id;
    private String skuCode;
    private Integer quantity;
    private String uom;
    private String grnNumber; // set once form-grn processes this item

    // Snapshotted at GRN-formation time (see MaterialReceiptServiceImpl.formGrn)
    private String batchNumber;
    private String location;
    private LocalDateTime expiryDate;
    private Integer inspectionQty;
}
