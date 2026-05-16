package com.aerospace.procurement_service.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
public class SupplierRequest {
    @NotBlank(message = "Supplier name is required")
    private String name;

    @NotBlank(message = "Vendor code is required")
    private String vendorCode;

    private String contactPerson;

    @Email(message="Invalid email format")
    private String Email;

    private String phone;
    private String address;

    @NotBlank(message = "Certification status is required")
    private String certificationStatus;

}
