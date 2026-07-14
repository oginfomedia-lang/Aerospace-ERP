package com.aerospace.inventory_service.dto;

import lombok.Data;

@Data
public class PartSupplierRequest {
    private Long supplierId;
    private boolean preferred;
}
