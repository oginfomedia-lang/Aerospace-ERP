package com.aerospace.procurement_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String vendorCode; // Internal unique code for the supplier

    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    // Structured address + tax registration — shown on the printed PO's Vendor
    // Address block, and used to decide CGST+SGST (same state as buyer) vs IGST
    // (different state) when computing tax on a PO.
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
    private String gstin;
    private String pan;
    private String gstinRegType; // e.g. "Full GST Registered", "Composition", "Unregistered"

    @Column(nullable = false)
    private String certificationStatus; // e.g., "CERTIFIED", "EXPIRED", "PENDING"

    private String rating; // e.g., "A", "B", "C" based on performance

    private String certificateNumber;      // e.g. "AS9100D-4471"
    private String certifyingBody;          // e.g. "NADCAP", "AS9100", "ISO 9001"
    private java.time.LocalDate certificationExpiryDate;

    // Soft delete (ARC-BR-003 pattern) — deactivated suppliers stay for history/audit
    // but are blocked from new POs
    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<PurchaseOrder> purchaseOrders;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @PrePersist
    private void stampCreatedBy() {
        String actor = currentActor();
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    @PreUpdate
    private void stampUpdatedBy() {
        this.updatedBy = currentActor();
    }

    private static String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
