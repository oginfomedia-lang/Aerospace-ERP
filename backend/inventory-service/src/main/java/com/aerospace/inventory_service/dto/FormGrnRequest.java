package com.aerospace.inventory_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

// Applied uniformly to every item on the MaterialReceipt when the GRN is formed —
// mirrors the single shared location/expiry the old direct-from-PO GRN form used.
@Data
public class FormGrnRequest {
    private String location;
    private LocalDateTime expiryDate;
    private Integer inspectionQty;
}
