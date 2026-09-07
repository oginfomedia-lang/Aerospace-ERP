package com.aerospace.finance_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.JournalLineCreateDTO;
import com.aerospace.finance_service.dto.SupplierPaymentCreateDTO;
import com.aerospace.finance_service.dto.SupplierPaymentDTO;
import com.aerospace.finance_service.entity.SupplierPayment;
import com.aerospace.finance_service.entity.SupplierPayment.PaymentStatus;
import com.aerospace.finance_service.entity.SupplierReference;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.SupplierPaymentRepository;
import com.aerospace.finance_service.repository.SupplierReferenceRepository;
import com.aerospace.finance_service.security.TenantContext;
import com.aerospace.finance_service.service.JournalEntryService;
import com.aerospace.finance_service.service.SupplierPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    // AP Control Account (Accounts Payable) — debit clears the liability
    private static final String AP_ACCOUNT  = "2000";
    // Bank / Cash account — credit reduces the bank balance
    private static final String BANK_ACCOUNT = "1100";

    private final SupplierPaymentRepository paymentRepository;
    private final SupplierReferenceRepository supplierReferenceRepository;
    private final JournalEntryService journalEntryService;
    private final DocumentSequenceService documentSequenceService;

    /**
     * Records a supplier payment and immediately posts the journal:
     *   Dr Accounts Payable (2000)   — clears the liability
     *   Cr Bank / Cash      (1100)   — reduces bank balance
     */
    @Override
    @Transactional
    public SupplierPaymentDTO create(SupplierPaymentCreateDTO dto, String currentUser) {
        // Resolve the real supplier FK. A miss here is a genuine distributed-systems
        // edge case (the supplier may exist in procurement-service but hasn't synced
        // into finance-service's local mirror yet via Kafka) — call that out in the
        // message rather than a bare "not found", since the caller likely picked a
        // valid id, not a wrong one.
        SupplierReference supplier = supplierReferenceRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier " + dto.getSupplierId() + " is not yet known to finance-service " +
                                "(it may not have synced from procurement-service yet)"));
        String derivedRef = "SUPPLIER:" + supplier.getId();

        // Build and post the payment journal entry
        JournalEntryCreateDTO journalDto = JournalEntryCreateDTO.builder()
                .postingDate(dto.getPaymentDate())
                .narration("Supplier payment to " + supplier.getSupplierName() +
                           " for invoice " + dto.getInvoiceNumber())
                .sourceDocType("SUPPLIER_PAYMENT")
                .sourceDocRef(dto.getInvoiceNumber())
                .costCenterId(dto.getCostCenterId())
                .lines(List.of(
                        JournalLineCreateDTO.builder()
                                .accountCode(AP_ACCOUNT)
                                .debitAmount(dto.getAmount())
                                .creditAmount(BigDecimal.ZERO)
                                .currency(dto.getCurrency())
                                .supplierId(supplier.getId())
                                .counterpartyRef(derivedRef)
                                .description("Payment clearing: " + dto.getInvoiceNumber())
                                .build(),
                        JournalLineCreateDTO.builder()
                                .accountCode(BANK_ACCOUNT)
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(dto.getAmount())
                                .currency(dto.getCurrency())
                                .description("Bank outflow: " + dto.getPaymentMethod())
                                .build()
                ))
                .build();

        JournalEntryDTO journalEntry = journalEntryService.create(journalDto, currentUser);
        // Auto-post (submit) the journal — small payments auto-post; large ones go to approval
        JournalEntryDTO posted = journalEntryService.submit(journalEntry.getId(), currentUser);

        SupplierPayment payment = SupplierPayment.builder()
                .tenantId(TenantContext.getTenantId())
                .paymentNumber(documentSequenceService.generate("PAY"))
                .invoiceNumber(dto.getInvoiceNumber())
                .supplier(supplier)
                .supplierRef(derivedRef)
                .paymentDate(dto.getPaymentDate())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .paymentMethod(dto.getPaymentMethod())
                .status(PaymentStatus.POSTED)
                .journalEntryRef(posted.getJournalNumber())
                .remarks(dto.getRemarks())
                .createdBy(currentUser)
                .build();

        return toDTO(paymentRepository.save(payment));
    }

    /** Cancel a payment: reverses the journal entry and marks the payment CANCELLED. */
    @Override
    @Transactional
    public SupplierPaymentDTO cancel(Long id, String currentUser) {
        // Row-locked fetch: without it, two concurrent cancel requests can both read
        // status != CANCELLED before either writes back, and both proceed to reverse
        // the journal — reverse() itself is now guarded (see JournalEntryServiceImpl),
        // but the "already cancelled" check here should be race-free in its own right too.
        SupplierPayment payment = paymentRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found: " + id));
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Payment is already cancelled");
        }
        // Reverse the journal entry that was posted when this payment was created
        if (payment.getJournalEntryRef() != null) {
            // Find the journal by journalNumber reference and reverse it
            journalEntryService.getAll().stream()
                    .filter(j -> payment.getJournalEntryRef().equals(j.getJournalNumber()))
                    .findFirst()
                    .ifPresent(j -> journalEntryService.reverse(j.getId(), currentUser,
                            "Payment " + payment.getPaymentNumber() + " cancelled"));
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        return toDTO(paymentRepository.save(payment));
    }

    @Override
    public List<SupplierPaymentDTO> getAll() {
        String tenantId = TenantContext.getTenantId();
        List<SupplierPayment> payments = tenantId != null
                ? paymentRepository.findByTenantId(tenantId)
                : paymentRepository.findAll();
        return payments.stream().map(this::toDTO).toList();
    }

    @Override
    public SupplierPaymentDTO getById(Long id) {
        return toDTO(getOrThrow(id));
    }

    private SupplierPayment getOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found: " + id));
    }

    private SupplierPaymentDTO toDTO(SupplierPayment p) {
        return SupplierPaymentDTO.builder()
                .id(p.getId()).paymentNumber(p.getPaymentNumber())
                .invoiceNumber(p.getInvoiceNumber())
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getSupplierName() : null)
                .supplierRef(p.getSupplierRef())
                .paymentDate(p.getPaymentDate()).amount(p.getAmount()).currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod()).status(p.getStatus())
                .journalEntryRef(p.getJournalEntryRef()).remarks(p.getRemarks())
                .createdBy(p.getCreatedBy()).createdAt(p.getCreatedAt()).build();
    }
}
