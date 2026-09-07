package com.aerospace.inventory_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MaterialReceiptRequest {
    @Size(max = 100)
    private String poNumber;
    @NotBlank
    @Size(max = 255)
    private String supplierName;
    @Size(max = 100)
    private String challanNumber;
    @Size(max = 255)
    private String transportName;
    @NotBlank
    @Size(max = 255)
    private String receivedBy;
    @NotEmpty
    @Valid
    private List<MaterialReceiptItemRequest> items;
}
