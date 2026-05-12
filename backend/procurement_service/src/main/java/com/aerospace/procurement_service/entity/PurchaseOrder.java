package com.aerospace.procurement_service.entity;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="purchase_orders")
@Data //generate getters, setters, toString, equals, hashCode methods
@Builder //Implements the Builder Pattern, allowing you to create objects like: PurchaseOrder.builder().poNumber("PO-1").build();.
@NoArgsConstructor //generate a no-args constructor, required by JPA
@AllArgsConstructor //generate an all-args constructor, useful for testing and manual object creation
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //eg. PO-2024-000051 - auto generated
    @Column(nullable = false, unique=true)
    private String poNumber;

    //supplier details
    @Column(nullable = false)
    private String supplierName;

    private String supplierCode; //vendor code
    private String supplierEmail;
    private String supplierPhone;
    private String supplierAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;

    //who raise the PO and who approved it
    private String createdBy;
    private String approvedBy;
    private String receivedBy;

    //financial
    private BigDecimal totalAmount;
    private String currency; //INR, USD, EUR

    //delivery details
    private String deliveryLocation;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime actualDeliveryDate;

    //linked to a low-stock event from inventory-service
    private Long triggerPartId; // the partId that triggered this PO
    private String triggerBatchNumber;
    private String remarks;
    private String cancellationReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;

    //one PO -> many line tems(one per part ordered)
    @OneToMany(mappedBy = "purchaseOrder",
               cascade = CascadeType.ALL, //if u save/delete the po, the line items are saved/deleted automatically
               orphanRemoval = true, //if a line item is removed from list, it is deleted from database
               fetch = FetchType.EAGER)
    @Builder.Default
    private List<PurchaseOrderLineItem> lineItems = new ArrayList<>();

    //@PrePersist / @PreUpdate: Lifecycle hooks that automatically set timestamps (createdAt, updatedAt) before 
    //the data is saved or changed.
    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if(this.status == null) this.status = PurchaseOrderStatus.DRAFT;
        if(this.currency == null) this.currency = "INR";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
