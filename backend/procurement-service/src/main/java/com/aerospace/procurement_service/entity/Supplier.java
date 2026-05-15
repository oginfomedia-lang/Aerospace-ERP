package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
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

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<PurchaseOrder> purchaseOrders;
}
