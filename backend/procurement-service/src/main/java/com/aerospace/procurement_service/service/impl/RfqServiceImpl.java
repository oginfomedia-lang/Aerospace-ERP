package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.RfqDTOs;
import com.aerospace.procurement_service.dto.RfqDTOs.*;
import com.aerospace.procurement_service.entity.*;
import com.aerospace.procurement_service.event.QuotationAwardedEvent;
import com.aerospace.procurement_service.event.QuotationRejectedEvent;
import com.aerospace.procurement_service.event.RfqCreatedEvent;
import com.aerospace.procurement_service.event.producer.ProcurementEventPublisher;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.repository.*;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import com.aerospace.procurement_service.service.PurchaseOrderService;
import com.aerospace.procurement_service.service.RfqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RfqServiceImpl implements RfqService {

    private final RequestForQuotationRepository rfqRepository;
    private final QuotationRepository quotationRepository;
    private final PurchaseRequisitionRepository prRepository;
    private final SupplierRepository supplierRepository;
    private final DocumentSequenceService documentSequenceService;
    private final ProcurementEventPublisher eventPublisher;
    private final PurchaseOrderService purchaseOrderService;

    @Override
    @Transactional
    public RfqResponse createRfq(Long prId, CreateRfqRequest request) {
        PurchaseRequisition pr = prRepository.findById(prId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Requisition not found: " + prId));

        Set<Supplier> suppliers = request.getSupplierIds().stream()
                .map(id -> supplierRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id)))
                .collect(Collectors.toSet());
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("Invite at least one supplier");
        }

        RequestForQuotation rfq = RequestForQuotation.builder()
                .rfqNumber(documentSequenceService.generate("RFQ"))
                .purchaseRequisition(pr)
                .invitedSuppliers(suppliers)
                .quantity(request.getQuantity())
                .specification(request.getSpecification())
                .dueDate(request.getDueDate())
                .status(RfqStatus.OPEN)
                .createdBy("PROCUREMENT_OFFICER")
                .build();
        RequestForQuotation saved = rfqRepository.save(rfq);

        pr.setStatus(PRStatus.RFQ_SENT);
        prRepository.save(pr);

        for (Supplier s : suppliers) {
            eventPublisher.publishRfqCreated(RfqCreatedEvent.builder()
                    .rfqId(saved.getId())
                    .rfqNumber(saved.getRfqNumber())
                    .partName(pr.getPartName())
                    .quantity(saved.getQuantity())
                    .dueDate(saved.getDueDate())
                    .supplierId(s.getId())
                    .supplierName(s.getName())
                    .supplierEmail(s.getEmail())
                    .build());
        }
        log.info("[RFQ] {} created for PR {} — invited {} suppliers",
                saved.getRfqNumber(), pr.getPrNumber(), suppliers.size());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public RfqResponse inviteMoreSuppliers(Long rfqId, InviteMoreSuppliersRequest request) {
        RequestForQuotation rfq = findRfqOrThrow(rfqId);
        if (rfq.getStatus() != RfqStatus.OPEN) {
            throw new IllegalStateException("Cannot invite more suppliers — RFQ is not OPEN (current status: "
                    + rfq.getStatus() + ")");
        }

        Set<Long> alreadyInvitedIds = rfq.getInvitedSuppliers().stream().map(Supplier::getId)
                .collect(Collectors.toSet());

        List<Supplier> newSuppliers = request.getSupplierIds().stream()
                .filter(id -> !alreadyInvitedIds.contains(id))
                .distinct()
                .map(id -> supplierRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id)))
                .toList();

        if (newSuppliers.isEmpty()) {
            throw new IllegalArgumentException("Every selected supplier is already invited to this RFQ");
        }

        rfq.getInvitedSuppliers().addAll(newSuppliers);
        RequestForQuotation saved = rfqRepository.save(rfq);

        PurchaseRequisition pr = rfq.getPurchaseRequisition();
        for (Supplier s : newSuppliers) {
            eventPublisher.publishRfqCreated(RfqCreatedEvent.builder()
                    .rfqId(saved.getId())
                    .rfqNumber(saved.getRfqNumber())
                    .partName(pr.getPartName())
                    .quantity(saved.getQuantity())
                    .dueDate(saved.getDueDate())
                    .supplierId(s.getId())
                    .supplierName(s.getName())
                    .supplierEmail(s.getEmail())
                    .build());
        }
        log.info("[RFQ] {} — invited {} additional supplier(s)", saved.getRfqNumber(), newSuppliers.size());
        return toDTO(saved);
    }

    @Override
    public List<RfqResponse> getAll() {
        return rfqRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    @Override
    public RfqResponse getById(Long id) {
        return toDTO(findRfqOrThrow(id));
    }

    @Override
    public List<QuotationResponse> getQuotations(Long rfqId) {
        return quotationRepository.findByRfq_IdOrderByUnitPriceAsc(rfqId).stream()
                .map(this::toQuotationDTO).toList();
    }

    @Override
    @Transactional
    public void selectWinner(Long rfqId, Long quotationId) {
        RequestForQuotation rfq = findRfqOrThrow(rfqId);
        if (rfq.getStatus() != RfqStatus.OPEN) {
            throw new IllegalStateException("RFQ is not OPEN — current status: " + rfq.getStatus());
        }

        List<Quotation> all = quotationRepository.findByRfq_IdOrderByUnitPriceAsc(rfqId);
        Quotation winner = all.stream().filter(q -> q.getId().equals(quotationId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found on this RFQ: " + quotationId));

        for (Quotation q : all) {
            boolean isWinner = q.getId().equals(quotationId);
            q.setStatus(isWinner ? QuotationStatus.SELECTED : QuotationStatus.REJECTED);
            quotationRepository.save(q);

            if (isWinner) {
                eventPublisher.publishQuotationAwarded(QuotationAwardedEvent.builder()
                        .rfqNumber(rfq.getRfqNumber())
                        .supplierId(q.getSupplier().getId())
                        .supplierName(q.getSupplier().getName())
                        .supplierEmail(q.getSupplier().getEmail())
                        .unitPrice(q.getUnitPrice())
                        .build());
            } else {
                eventPublisher.publishQuotationRejected(QuotationRejectedEvent.builder()
                        .rfqNumber(rfq.getRfqNumber())
                        .supplierId(q.getSupplier().getId())
                        .supplierName(q.getSupplier().getName())
                        .supplierEmail(q.getSupplier().getEmail())
                        .build());
            }
        }

        rfq.setStatus(RfqStatus.AWARDED);
        rfqRepository.save(rfq);

        PurchaseRequisition pr = rfq.getPurchaseRequisition();
        pr.setStatus(PRStatus.SUPPLIER_SELECTED);
        prRepository.save(pr);

        // Real PO — real price, real supplier, no more $0 guess.
        purchaseOrderService.createFromAwardedQuotation(pr, rfq, winner);

        pr.setStatus(PRStatus.PO_RAISED);
        prRepository.save(pr);

        log.info("[RFQ] {} awarded to supplier {} at {}", rfq.getRfqNumber(),
                winner.getSupplier().getName(), winner.getUnitPrice());
    }

    // ── Supplier portal ──────────────────────────────────────────────────────

    @Override
    public List<RfqResponse> getOpenRfqsForSupplier(Long supplierId) {
        return rfqRepository.findByInvitedSuppliers_Id(supplierId).stream()
                .filter(r -> r.getStatus() == RfqStatus.OPEN)
                .map(this::toDTO)
                .toList();
    }

    @Override
    public RfqResponse getRfqForSupplier(Long rfqId, Long supplierId) {
        RequestForQuotation rfq = findRfqOrThrow(rfqId);
        assertInvited(rfq, supplierId);
        return toDTO(rfq);
    }

    @Override
    @Transactional
    public QuotationResponse submitQuotation(Long rfqId, Long supplierId, SubmitQuotationRequest request) {
        RequestForQuotation rfq = findRfqOrThrow(rfqId);
        assertInvited(rfq, supplierId);
        if (rfq.getStatus() != RfqStatus.OPEN) {
            throw new IllegalStateException("This RFQ is no longer open for quotes");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));

        BigDecimal total = request.getUnitPrice().multiply(BigDecimal.valueOf(rfq.getQuantity()));

        Quotation quotation = quotationRepository.findByRfq_IdAndSupplier_Id(rfqId, supplierId)
                .orElse(Quotation.builder().rfq(rfq).supplier(supplier).status(QuotationStatus.SUBMITTED).build());

        quotation.setUnitPrice(request.getUnitPrice());
        quotation.setTotalPrice(total);
        quotation.setLeadTimeDays(request.getLeadTimeDays());
        quotation.setValidityDate(request.getValidityDate());
        quotation.setRemarks(request.getRemarks());
        quotation.setStatus(QuotationStatus.SUBMITTED);

        return toQuotationDTO(quotationRepository.save(quotation));
    }

    @Override
    public List<QuotationResponse> getMyQuotations(Long supplierId) {
        return quotationRepository.findBySupplier_IdOrderBySubmittedAtDesc(supplierId).stream()
                .map(this::toQuotationDTO).toList();
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private RequestForQuotation findRfqOrThrow(Long id) {
        return rfqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + id));
    }

    private void assertInvited(RequestForQuotation rfq, Long supplierId) {
        boolean invited = rfq.getInvitedSuppliers().stream().anyMatch(s -> s.getId().equals(supplierId));
        if (!invited) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You were not invited to this RFQ");
        }
    }

    private RfqResponse toDTO(RequestForQuotation rfq) {
        List<Quotation> quotes = quotationRepository.findByRfq_IdOrderByUnitPriceAsc(rfq.getId());
        List<RfqDTOs.InvitedSupplierSummary> invited = rfq.getInvitedSuppliers().stream()
                .map(s -> RfqDTOs.InvitedSupplierSummary.builder()
                        .supplierId(s.getId())
                        .supplierName(s.getName())
                        .hasQuoted(quotes.stream().anyMatch(q -> q.getSupplier().getId().equals(s.getId())))
                        .build())
                .toList();
        return RfqResponse.builder()
                .id(rfq.getId())
                .rfqNumber(rfq.getRfqNumber())
                .purchaseRequisitionId(rfq.getPurchaseRequisition().getId())
                .partName(rfq.getPurchaseRequisition().getPartName())
                .skuCode(rfq.getPurchaseRequisition().getSkuCode())
                .invitedSuppliers(invited)
                .quantity(rfq.getQuantity())
                .specification(rfq.getSpecification())
                .dueDate(rfq.getDueDate())
                .status(rfq.getStatus())
                .createdAt(rfq.getCreatedAt())
                .build();
    }

    private QuotationResponse toQuotationDTO(Quotation q) {
        return QuotationResponse.builder()
                .id(q.getId())
                .rfqId(q.getRfq().getId())
                .rfqNumber(q.getRfq().getRfqNumber())
                .partName(q.getRfq().getPurchaseRequisition().getPartName())
                .supplierId(q.getSupplier().getId())
                .supplierName(q.getSupplier().getName())
                .unitPrice(q.getUnitPrice())
                .totalPrice(q.getTotalPrice())
                .leadTimeDays(q.getLeadTimeDays())
                .validityDate(q.getValidityDate())
                .remarks(q.getRemarks())
                .status(q.getStatus())
                .submittedAt(q.getSubmittedAt())
                .build();
    }
}