package com.aerospace.sales_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String billingAddress;
    private String shippingAddress;
    private String currency;
    private String paymentTerms;
    private String status; // ACTIVE, INACTIVE, ON_HOLD — ignored on create, always forced ACTIVE
}