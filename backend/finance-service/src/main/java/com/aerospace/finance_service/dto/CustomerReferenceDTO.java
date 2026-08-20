package com.aerospace.finance_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerReferenceDTO {
    private Long id;
    private String customerName;
    private String customerCode;
    private String status;
}
