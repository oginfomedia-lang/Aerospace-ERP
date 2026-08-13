package com.aerospace.sales_service.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponseDTO {
    private Long id;
    private String customerCode;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String billingAddress;
    private String shippingAddress;
    private String currency;
    private String paymentTerms;
    private String status;
    private Long version;
    private LocalDateTime createdAt;
}