package com.aerospace.procurement_service.dto;

import lombok.Data;

@Data
public class OrganizationProfileRequest {
    private String companyName;
    private String addressLine;
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
    private String phone;
    private String telefax;
    private String email;

    private String gstin;
    private String pan;

    private String range;
    private String division;

    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
    private String bankIfsc;

    private String defaultPaymentTerms;
    private String defaultPayment;
    private String defaultDeliveryTerms;
    private String defaultExciseDuty;
    private String termsAndConditions;
}
