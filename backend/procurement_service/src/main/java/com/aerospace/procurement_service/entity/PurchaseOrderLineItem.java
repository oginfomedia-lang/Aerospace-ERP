package com.aerospace.procurement_service.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "po_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="purchase_order_id", nullable = false)
    @ToString.Exclude //to prevent circular reference in toString()
    private PurchaseOrder purchaseOrder;

    //refreneces inventory-service part
    @Column(nullable = false)
    private Long partId;

    private String partName;
    private String partNumber;

    @Column(nullable = false)
    private Integer quantityOrdered;

    private Integer quantityReceived; //filled on recepit

    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    private String batchNumber;
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineItemStatus status;

    private String remarks;

    public enum LineItemStatus {
        PENDING,
        PARTIALLY_RECEIVED,
        RECEIVED,
        CANCELLED
    }


    
}
