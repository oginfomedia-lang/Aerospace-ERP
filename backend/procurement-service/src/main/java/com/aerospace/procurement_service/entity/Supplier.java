package com.aerospace.procurement_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

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

    @Column(nullable = false)
    private String certificationStatus; // e.g., "CERTIFIED", "EXPIRED", "PENDING"

    private String rating; // e.g., "A", "B", "C" based on performance

    // Soft delete (ARC-BR-003 pattern) — deactivated suppliers stay for history/audit
    // but are blocked from new POs
    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<PurchaseOrder> purchaseOrders;
}
