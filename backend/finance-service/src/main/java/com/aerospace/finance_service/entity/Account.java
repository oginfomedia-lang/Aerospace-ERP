package com.aerospace.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chart_of_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multi-tenant isolation: every record belongs to exactly one tenant.
    // Stamped by service layer from TenantContext on creation; never changes after that.
    @Column(length = 50)
    private String tenantId;

    @Column(nullable = false, unique = true, length = 30)
    private String accountCode;

    @Column(nullable = false, length = 150)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE

    // self-reference for the hierarchy — a plain JPA @ManyToOne back onto the same table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @Column(length = 100)
    private String financialCategory; // e.g. "Current Asset" — free-text grouping for reports

    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(length = 200)
    private String currencyRestrictions; // comma-separated ISO codes; null = any currency

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostingPermission postingPermission = PostingPermission.POSTABLE; // or HEADER_ONLY (group node)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum AccountType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
    public enum PostingPermission { POSTABLE, HEADER_ONLY }
}
