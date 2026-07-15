package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.DebitNoteResponse;
import com.aerospace.procurement_service.entity.DebitNote;
import com.aerospace.procurement_service.entity.DebitNoteStatus;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.procurement_service.event.VendorReturnEvent;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.mapper.DebitNoteMapper;
import com.aerospace.procurement_service.repository.DebitNoteRepository;
import com.aerospace.procurement_service.repository.PurchaseOrderRepository;
import com.aerospace.procurement_service.service.DebitNoteService;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebitNoteServiceImpl implements DebitNoteService {

    private final DebitNoteRepository debitNoteRepository;
    private final PurchaseOrderRepository poRepository;
    private final DocumentSequenceService documentSequenceService;
    private final DebitNoteMapper mapper;

    // Kafka-driven, like PurchaseOrderServiceImpl.applyQaOutcome — closes the loop from
    // "rejected and sent back" to "the supplier owes us credit for it."
    @Override
    @Transactional
    public void createFromVendorReturn(VendorReturnEvent event) {
        if (event.getPoNumber() == null || event.getPoNumber().isBlank()) {
            log.warn("[DEBIT-NOTE] Vendor return for NCR {} has no traceable PO — skipping debit note",
                    event.getNcrNumber());
            return; // not traceable to a PO (e.g. manual stock add) — nothing to bill back
        }

        PurchaseOrder po = poRepository.findByPoNumber(event.getPoNumber()).orElse(null);
        if (po == null) {
            log.warn("[DEBIT-NOTE] No PO found for {} — skipping debit note for NCR {}",
                    event.getPoNumber(), event.getNcrNumber());
            return;
        }

        POLineItem line = po.getItems().stream()
                .filter(i -> i.getSkuCode().equals(event.getSkuCode()))
                .findFirst().orElse(null);
        BigDecimal unitPrice = line != null && line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
        int qty = event.getQuantity() != null ? event.getQuantity() : 0;
        BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty));

        DebitNote note = DebitNote.builder()
                .debitNoteNumber(documentSequenceService.generate("DN"))
                .purchaseOrder(po)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : null)
                .skuCode(event.getSkuCode())
                .quantity(qty)
                .unitPrice(unitPrice)
                .amount(amount)
                .ncrNumber(event.getNcrNumber())
                .batchNumber(event.getBatchNumber())
                .status(DebitNoteStatus.OPEN)
                .remarks(event.getRemarks())
                .build();

        debitNoteRepository.save(note);
        log.info("📢 [PROCUREMENT] Debit Note {} raised against {} for {} units of {} (NCR {})",
                note.getDebitNoteNumber(), note.getSupplierName(), qty, event.getSkuCode(), event.getNcrNumber());
    }

    @Override
    public List<DebitNoteResponse> getAll() {
        return debitNoteRepository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DebitNoteResponse markSettled(Long id) {
        DebitNote note = debitNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debit Note not found with id " + id));
        if (note.getStatus() != DebitNoteStatus.OPEN) {
            throw new IllegalStateException("Only an OPEN debit note can be marked settled (current: " + note.getStatus() + ").");
        }
        note.setStatus(DebitNoteStatus.SETTLED);
        note.setSettledAt(LocalDateTime.now());
        return mapper.toDTO(debitNoteRepository.save(note));
    }
}