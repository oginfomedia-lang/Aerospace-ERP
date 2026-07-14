package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;

// Singleton row (id is always fixed at 1 — see OrganizationProfileServiceImpl) holding
// the "who are we" letterhead/tax/banking info and default document boilerplate reused
// on every printed Purchase Order.
@Entity
@Table(name = "organization_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationProfile {
    @Id
    private Long id;

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

    // Central Excise registration fields shown on the PO header
    private String range;
    private String division;

    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
    private String bankIfsc;

    // Document defaults — a PO can override any of these; null on the PO falls back
    // to these values at render time (see PurchaseOrder.paymentTerms etc.)
    @Column(length = 1000)
    private String defaultPaymentTerms;
    @Column(length = 1000)
    private String defaultPayment;
    @Column(length = 1000)
    private String defaultDeliveryTerms;
    @Column(length = 1000)
    private String defaultExciseDuty;
    @Column(length = 4000)
    private String termsAndConditions;
}
