package com.aerospace.finance_service.service.impl;

import com.aerospace.common.approval.ApprovalEngine;
import com.aerospace.common.approval.ApprovalOutcome;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.finance_service.dto.JournalEntryCreateDTO;
import com.aerospace.finance_service.dto.JournalEntryDTO;
import com.aerospace.finance_service.dto.JournalLineDTO;
import com.aerospace.finance_service.dto.JournalLineCreateDTO;
import com.aerospace.finance_service.entity.Account;
import com.aerospace.finance_service.entity.JournalEntry;
import com.aerospace.finance_service.entity.JournalEntry.JournalStatus;
import com.aerospace.finance_service.entity.JournalLine;
import com.aerospace.finance_service.entity.SupplierReference;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.AccountRepository;
import com.aerospace.finance_service.repository.JournalEntryRepository;
import com.aerospace.finance_service.repository.SupplierReferenceRepository;
import com.aerospace.finance_service.service.AccountingPeriodService;
import com.aerospace.finance_service.service.JournalEntryService;
import com.aerospace.finance_service.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalEntryServiceImpl implements JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final SupplierReferenceRepository supplierReferenceRepository;
    private final DocumentSequenceService documentSequenceService;
    private final AccountingPeriodService accountingPeriodService;
    private final ApprovalEngine approvalEngine;

    // journals at/above this amount require approval before posting; below it,
    // submit auto-posts immediately — mirrors ApprovalMatrix's own "no matching
    // band = auto-approve" behavior, applied at the call site so small journals
    // don't even round-trip through the approval tables.
    @Value("${finance.journal.approval-threshold:0}")
    private BigDecimal approvalThreshold;

    private static final String MODULE_CODE = "JOURNAL_ENTRY";

    @Override
    @Transactional
    public JournalEntryDTO create(JournalEntryCreateDTO dto, String currentUser) {
        accountingPeriodService.assertOpenForPosting(dto.getPostingDate());

        JournalEntry entry = JournalEntry.builder()
                .journalNumber(documentSequenceService.generate("JE"))
                .postingDate(dto.getPostingDate())
                .narration(dto.getNarration())
                .sourceDocType(dto.getSourceDocType())
                .sourceDocRef(dto.getSourceDocRef())
                .costCenterId(dto.getCostCenterId())
                .profitCenterId(dto.getProfitCenterId())
                .organizationId(dto.getOrganizationId())
                .plantId(dto.getPlantId())
                .status(JournalStatus.DRAFT)
                .createdBy(currentUser)
                .tenantId(com.aerospace.finance_service.security.TenantContext.getTenantId())
                .build();

        assertPostableAccounts(dto.getLines());
        dto.getLines().forEach(l -> entry.getLines().add(buildLine(entry, l)));

        assertBalanced(entry);
        return toDTO(journalEntryRepository.save(entry));
    }

    /**
     * Builds one JournalLine from its create-DTO, resolving an optional supplierId
     * into a real SupplierReference FK and deriving counterpartyRef from it when the
     * caller didn't supply one explicitly. Shared by create()/update() so both stay
     * in sync — a line built by either path behaves identically to AP ledger/aging/
     * revaluation queries downstream, which still group by counterpartyRef.
     */
    private JournalLine buildLine(JournalEntry entry, JournalLineCreateDTO l) {
        SupplierReference supplier = null;
        String counterpartyRef = l.getCounterpartyRef();
        if (l.getSupplierId() != null) {
            supplier = supplierReferenceRepository.findById(l.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Supplier " + l.getSupplierId() + " is not yet known to finance-service " +
                                    "(it may not have synced from procurement-service yet)"));
            if (counterpartyRef == null) {
                counterpartyRef = "SUPPLIER:" + supplier.getId();
            }
        }
        return JournalLine.builder().journalEntry(entry).accountCode(l.getAccountCode())
                .debitAmount(nvl(l.getDebitAmount())).creditAmount(nvl(l.getCreditAmount()))
                .currency(l.getCurrency()).counterpartyRef(counterpartyRef).supplier(supplier)
                .costCenterId(l.getCostCenterId()).profitCenterId(l.getProfitCenterId())
                .description(l.getDescription()).build();
    }

    /**
     * Every line's accountCode must reference a real, active, POSTABLE account — this
     * was previously never checked, which is how a journal could end up posted against
     * an account that's inactive (or a HEADER_ONLY rollup account never meant to carry
     * postings directly) with nothing rejecting it.
     */
    private void assertPostableAccounts(List<JournalLineCreateDTO> lines) {
        for (JournalLineCreateDTO line : lines) {
            Account account = accountRepository.findByAccountCode(line.getAccountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Account code does not exist: " + line.getAccountCode()));
            if (!Boolean.TRUE.equals(account.getActiveStatus())) {
                throw new IllegalArgumentException("Account " + line.getAccountCode() + " is inactive and cannot be posted to");
            }
            if (account.getPostingPermission() == Account.PostingPermission.HEADER_ONLY) {
                throw new IllegalArgumentException("Account " + line.getAccountCode() + " is a header account and cannot be posted to directly");
            }
        }
    }

    /**
     * THE core double-entry rule: total debits must equal total credits, per currency
     * (a multi-currency journal — e.g. an FX revaluation entry — is allowed, but each
     * currency bucket within it must independently balance to zero).
     */
    private void assertBalanced(JournalEntry entry) {
        if (entry.getLines().size() < 2) {
            throw new IllegalArgumentException("A journal entry needs at least two lines");
        }
        var byCurrency = entry.getLines().stream().collect(Collectors.groupingBy(JournalLine::getCurrency));
        for (var e : byCurrency.entrySet()) {
            BigDecimal debit = e.getValue().stream().map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = e.getValue().stream().map(JournalLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (debit.compareTo(credit) != 0) {
                throw new IllegalArgumentException(String.format(
                        "Journal entry is not balanced for currency %s: debits=%s credits=%s", e.getKey(), debit, credit));
            }
        }
    }

    /**
     * Immutability enforcement (ADR-DDS-057): editing a POSTED journal is rejected
     * outright. The only legitimate way to change a posted entry's effect is a new
     * reversing entry (reverse() below) — never edit history.
     */
    @Override
    @Transactional
    public JournalEntryDTO update(Long id, JournalEntryCreateDTO dto) {
        JournalEntry entry = getEntityOrThrow(id);
        if (entry.getStatus() == JournalStatus.POSTED) {
            throw new IllegalStateException("Journal " + entry.getJournalNumber() +
                    " is POSTED and immutable. Create a reversing entry instead.");
        }
        if (entry.getStatus() == JournalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Journal " + entry.getJournalNumber() +
                    " is pending approval and cannot be edited. Reject it first.");
        }
        assertPostableAccounts(dto.getLines());
        entry.getLines().clear();
        entry.setNarration(dto.getNarration());
        entry.setPostingDate(dto.getPostingDate());
        dto.getLines().forEach(l -> entry.getLines().add(buildLine(entry, l)));
        assertBalanced(entry);
        return toDTO(journalEntryRepository.save(entry));
    }

    /**
     * Design decision: approval and posting are the SAME step — once ApprovalEngine
     * reports fully approved, the journal posts immediately (see approve() below),
     * rather than a separate manual "post" click. Same 3-state flow SalesOrder's own
     * approval already uses: submit -> approve -> [auto] committed.
     */
    @Override
    @Transactional
    public JournalEntryDTO submit(Long id, String currentUser) {
        JournalEntry entry = getEntityOrThrow(id);
        if (entry.getStatus() != JournalStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT journals can be submitted");
        }
        BigDecimal totalAmount = entry.getLines().stream()
                .map(JournalLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(approvalThreshold) < 0) {
            entry.setStatus(JournalStatus.POSTED);
            entry.setApprovedBy(currentUser);
            entry.setApprovedAt(LocalDateTime.now());
            entry.setPostedAt(LocalDateTime.now());
            return toDTO(journalEntryRepository.save(entry));
        }

        entry.setStatus(JournalStatus.PENDING_APPROVAL);
        journalEntryRepository.save(entry);
        approvalEngine.submitForApproval(MODULE_CODE, entry.getId(), totalAmount, 1);
        return toDTO(entry);
    }

    @Override
    @Transactional
    public JournalEntryDTO approve(Long id, String approverUsername, Set<String> approverRoles, String remarks) {
        JournalEntry entry = getEntityOrThrow(id);
        if (entry.getStatus() != JournalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Journal is not pending approval");
        }
        // ApprovalEngine's own resolveActingRow already skips the acting candidate whose
        // approverUsername matches the document's own creator — segregation of duties
        // is inherited for free, same as PurchaseOrder/SalesOrder approval.
        ApprovalOutcome outcome = approvalEngine.approve(MODULE_CODE, entry.getId(), approverUsername, approverRoles, remarks);
        if (outcome.status() == ApprovalOutcome.Status.FULLY_APPROVED) {
            entry.setStatus(JournalStatus.POSTED);
            entry.setApprovedBy(approverUsername);
            entry.setApprovedAt(LocalDateTime.now());
            entry.setPostedAt(LocalDateTime.now());
        }
        return toDTO(journalEntryRepository.save(entry));
    }

    @Override
    @Transactional
    public JournalEntryDTO reject(Long id, String approverUsername, Set<String> approverRoles, String remarks) {
        JournalEntry entry = getEntityOrThrow(id);
        if (entry.getStatus() != JournalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Journal is not pending approval");
        }
        approvalEngine.reject(MODULE_CODE, entry.getId(), approverUsername, approverRoles, remarks);
        entry.setStatus(JournalStatus.REJECTED);
        return toDTO(journalEntryRepository.save(entry));
    }

    /**
     * Reversing entry: the ONLY sanctioned way to "undo" a posted journal. Builds a
     * brand-new journal with debit/credit swapped on every line, dated today, auto-
     * posted (a reversal of already-vetted money doesn't need a second approval round
     * in this design). Both entries are cross-linked for traceability.
     */
    @Override
    @Transactional
    public JournalEntryDTO reverse(Long id, String currentUser, String reason) {
        JournalEntry original = getEntityOrThrow(id);
        if (original.getStatus() != JournalStatus.POSTED) {
            throw new IllegalStateException("Only POSTED journals can be reversed");
        }
        accountingPeriodService.assertOpenForPosting(LocalDate.now());

        JournalEntry reversal = JournalEntry.builder()
                .journalNumber(documentSequenceService.generate("JE"))
                .postingDate(LocalDate.now())
                .narration("Reversal of " + original.getJournalNumber() + ": " + reason)
                .sourceDocType("REVERSAL").sourceDocRef(original.getJournalNumber())
                .costCenterId(original.getCostCenterId()).profitCenterId(original.getProfitCenterId())
                .status(JournalStatus.POSTED)
                .createdBy(currentUser).approvedBy(currentUser)
                .approvedAt(LocalDateTime.now()).postedAt(LocalDateTime.now())
                .reversalOfJournalId(original.getId())
                .build();

        original.getLines().forEach(l -> reversal.getLines().add(
                JournalLine.builder().journalEntry(reversal).accountCode(l.getAccountCode())
                        .debitAmount(l.getCreditAmount()).creditAmount(l.getDebitAmount()) // swapped
                        .currency(l.getCurrency()).counterpartyRef(l.getCounterpartyRef()).supplier(l.getSupplier())
                        .costCenterId(l.getCostCenterId()).profitCenterId(l.getProfitCenterId())
                        .description("Reversal: " + l.getDescription()).build()));

        assertBalanced(reversal);
        JournalEntry saved = journalEntryRepository.save(reversal);
        original.setReversedByJournalId(saved.getId());
        journalEntryRepository.save(original);
        return toDTO(saved);
    }

    private JournalEntry getEntityOrThrow(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found: " + id));
    }

    private BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    @Override
    public JournalEntryDTO getById(Long id) { return toDTO(getEntityOrThrow(id)); }

    @Override
    public List<JournalEntryDTO> getAll() { return journalEntryRepository.findAll().stream().map(this::toDTO).toList(); }

    private JournalEntryDTO toDTO(JournalEntry e) {
        return JournalEntryDTO.builder()
                .id(e.getId()).journalNumber(e.getJournalNumber()).postingDate(e.getPostingDate())
                .narration(e.getNarration()).sourceDocType(e.getSourceDocType()).sourceDocRef(e.getSourceDocRef())
                .status(e.getStatus()).createdBy(e.getCreatedBy()).approvedBy(e.getApprovedBy())
                .approvedAt(e.getApprovedAt()).postedAt(e.getPostedAt())
                .reversalOfJournalId(e.getReversalOfJournalId()).reversedByJournalId(e.getReversedByJournalId())
                .lines(e.getLines().stream().map(l -> JournalLineDTO.builder()
                        .id(l.getId()).accountCode(l.getAccountCode())
                        .debitAmount(l.getDebitAmount()).creditAmount(l.getCreditAmount())
                        .currency(l.getCurrency()).counterpartyRef(l.getCounterpartyRef())
                        .supplierId(l.getSupplier() != null ? l.getSupplier().getId() : null)
                        .costCenterId(l.getCostCenterId()).profitCenterId(l.getProfitCenterId())
                        .description(l.getDescription()).build()).toList())
                .build();
    }
}
