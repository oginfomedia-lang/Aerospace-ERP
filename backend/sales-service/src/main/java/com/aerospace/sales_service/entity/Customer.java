package com.aerospace.sales_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// One row per customer organization. customerCode is auto-generated (never typed by a
// user) via DocumentSequenceService, the same way every other "code" field in this app
// works (EmployeeCode, PoNumber, WoNumber, ...) — see CustomerServiceImpl.registerCustomer.
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String customerCode; // e.g. CUST-2026-000001

    @Column(nullable = false)
    private String name;

    private String contactPerson;
    private String email;
    private String phone;

    private String billingAddress;
    private String shippingAddress;

    private String currency;      // e.g. "USD" — defaults to org currency if left blank
    private String paymentTerms;  // free text, e.g. "Net 30"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    // Optimistic locking — same reasoning as Machine/Employee/RoutingTemplate: two sales
    // staff editing the same customer at once now fail loudly instead of silently
    // overwriting each other.
    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}