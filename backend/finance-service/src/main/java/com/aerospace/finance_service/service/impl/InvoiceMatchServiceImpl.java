package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.InvoiceMatchDTO;
import com.aerospace.finance_service.dto.InvoiceMatchRequestDTO;
import com.aerospace.finance_service.entity.InvoiceMatch;
import com.aerospace.finance_service.entity.InvoiceMatch.MatchStatus;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.InvoiceMatchRepository;
import com.aerospace.finance_service.security.TenantContext;
import com.aerospace.finance_service.service.InvoiceMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceMatchServiceImpl implements InvoiceMatchService {

    private final InvoiceMatchRepository invoiceMatchRepository;

    /**
     * 3-way matching logic:
     *   1. If all three amounts are equal within tolerance (0.01) → MATCHED
     *   2. If GRN amount < PO amount (partial delivery) → PARTIAL
     *   3. If invoice amount > GRN amount (over-invoiced) → DISCREPANCY
     *
     * Tolerance of 0.01 handles minor rounding differences between systems.
     */
    @Override
    @Transactional
    public InvoiceMatchDTO match(InvoiceMatchRequestDTO req, String currentUser) {
        BigDecimal tolerance = new BigDecimal("0.01");

        MatchStatus status;
        String notes = null;

        if (req.getInvoiceAmount().subtract(req.getGrnAmount()).abs().compareTo(tolerance) <= 0
                && req.getGrnAmount().subtract(req.getPoAmount()).abs().compareTo(tolerance) <= 0) {
            status = MatchStatus.MATCHED;
        } else if (req.getGrnAmount().compareTo(req.getPoAmount()) < 0) {
            status = MatchStatus.PARTIAL;
            notes = String.format("GRN amount %.2f < PO amount %.2f — partial delivery received.",
                    req.getGrnAmount(), req.getPoAmount());
            if (req.getInvoiceAmount().subtract(req.getGrnAmount()).compareTo(tolerance) > 0) {
                status = MatchStatus.DISCREPANCY;
                notes += String.format(" Invoice %.2f exceeds GRN %.2f — over-invoiced.",
                        req.getInvoiceAmount(), req.getGrnAmount());
            }
        } else {
            status = MatchStatus.DISCREPANCY;
            notes = String.format(
                    "Invoice amount %.2f does not match GRN %.2f or PO %.2f — requires resolution.",
                    req.getInvoiceAmount(), req.getGrnAmount(), req.getPoAmount());
        }

        InvoiceMatch record = InvoiceMatch.builder()
                .tenantId(TenantContext.getTenantId())
                .poNumber(req.getPoNumber()).grnNumber(req.getGrnNumber())
                .invoiceNumber(req.getInvoiceNumber())
                .poAmount(req.getPoAmount()).grnAmount(req.getGrnAmount())
                .invoiceAmount(req.getInvoiceAmount())
                .matchStatus(status).discrepancyNotes(notes)
                .matchedBy(currentUser).build();

        return toDTO(invoiceMatchRepository.save(record));
    }

    @Override
    public List<InvoiceMatchDTO> getAll() {
        String tenantId = TenantContext.getTenantId();
        List<InvoiceMatch> records = tenantId != null
                ? invoiceMatchRepository.findByTenantId(tenantId)
                : invoiceMatchRepository.findAll();
        return records.stream().map(this::toDTO).toList();
    }

    @Override
    public InvoiceMatchDTO getById(Long id) {
        return toDTO(invoiceMatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice match not found: " + id)));
    }

    private InvoiceMatchDTO toDTO(InvoiceMatch m) {
        return InvoiceMatchDTO.builder()
                .id(m.getId()).poNumber(m.getPoNumber()).grnNumber(m.getGrnNumber())
                .invoiceNumber(m.getInvoiceNumber())
                .poAmount(m.getPoAmount()).grnAmount(m.getGrnAmount())
                .invoiceAmount(m.getInvoiceAmount())
                .matchStatus(m.getMatchStatus()).discrepancyNotes(m.getDiscrepancyNotes())
                .matchedAt(m.getMatchedAt()).matchedBy(m.getMatchedBy()).build();
    }
}
