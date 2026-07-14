package com.aerospace.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lightweight Part projection for the Supplier page's "Products Supplied" reverse
// lookup — the full PartResponseDTO requires StockBalance aggregation this doesn't need.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartBasicDTO {
    private Long id;
    private String name;
    private String skuCode;
    private boolean preferred;
}
