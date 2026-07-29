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
    private String email;

    private String phone;
    private String address;

    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
    private String gstin;
    private String pan;
    private String gstinRegType;

    @NotBlank(message = "Certification status is required")
    private String certificationStatus;

    private String rating;

    private String certificateNumber;
    private String certifyingBody;
    private java.time.LocalDate certificationExpiryDate;

}
