package com.aerospace.finance_service.dto;

import com.aerospace.finance_service.entity.InvoiceMatch.MatchStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceMatchDTO {
    private Long id;
    private String poNumber;
    private String grnNumber;
    private String invoiceNumber;
    private BigDecimal poAmount;
    private BigDecimal grnAmount;
    private BigDecimal invoiceAmount;
    private MatchStatus matchStatus;
    private String discrepancyNotes;
    private LocalDateTime matchedAt;
    private String matchedBy;
}
