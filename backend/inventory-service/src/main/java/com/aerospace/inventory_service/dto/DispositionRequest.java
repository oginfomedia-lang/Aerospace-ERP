package com.aerospace.inventory_service.dto;

import lombok.Data;

@Data
public class DispositionRequest {
    private String dispositionType; // RETURN_TO_VENDOR | SCRAP | USE_AS_IS
    private String remarks;
    private String dispositionedBy; // stand-in for the special-approval signature on USE_AS_IS
}
