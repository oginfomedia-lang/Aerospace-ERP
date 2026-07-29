package com.aerospace.procurement_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "requests_for_quotation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestForQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private String rfqNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    private PurchaseRequisition purchaseRequisition;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "rfq_invited_suppliers", joinColumns = @JoinColumn(name = "rfq_id"), inverseJoinColumns = @JoinColumn(name = "supplier_id"))
    private Set<Supplier> invitedSuppliers = new HashSet<>();

    @Column(nullable = false)
    private Integer quantity;

    private String specification;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RfqStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quotation> quotations = new ArrayList<>();

    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
