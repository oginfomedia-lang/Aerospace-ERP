package com.aerospace.procurement_service.service.impl;

import com.aerospace.procurement_service.dto.InvoiceActionRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceLineItemDTO;
import com.aerospace.procurement_service.dto.VendorInvoiceRequest;
import com.aerospace.procurement_service.dto.VendorInvoiceResponse;
import com.aerospace.procurement_service.entity.InvoiceStatus;
import com.aerospace.procurement_service.entity.OrganizationProfile;
import com.aerospace.procurement_service.entity.POLineItem;
import com.aerospace.procurement_service.entity.PurchaseOrder;
import com.aerospace.procurement_service.entity.VendorInvoice;
import com.aerospace.procurement_service.entity.VendorInvoiceLineItem;
import com.aerospace.procurement_service.exception.ResourceNotFoundException;
import com.aerospace.procurement_service.mapper.VendorInvoiceMapper;
import com.aerospace.procurement_service.repository.PurchaseOrderRepository;
import com.aerospace.procurement_service.repository.VendorInvoiceRepository;
import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.common.approval.ApprovalEngine;
import com.aerospace.common.approval.ApprovalOutcome;
import com.aerospace.procurement_service.service.OrganizationProfileService;
import com.aerospace.procurement_service.service.VendorInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Three-way match: a vendor invoice's lines are matched against the PO's negotiated
// unitPrice and the QA-accepted quantity already sitting on POLineItem (synced from
// inventory-service over Kafka — no cross-service call needed here). See the plan doc
// for the full design rationale.
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorInvoiceServiceImpl implements VendorInvoiceService {

    private static final String MODULE = "VENDOR_INVOICE";

    private final VendorInvoiceRepository invoiceRepository;
    private final PurchaseOrderRepository poRepository;
    private final OrganizationProfileService organizationProfileService;
    private final DocumentSequenceService documentSequenceService;
    private final ApprovalEngine approvalEngine;
    private final VendorInvoiceMapper mapper;

    @Override
    @Transactional
    public VendorInvoiceResponse createAndMatch(VendorInvoiceRequest request) {
        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id " + request.getPurchaseOrderId()));

        OrganizationProfile org = organizationProfileService.getProfile();
        BigDecimal priceTolerancePercent = org.getInvoicePriceTolerancePercent() != null
                ? org.getInvoicePriceTolerancePercent() : BigDecimal.ZERO;

        VendorInvoice invoice = VendorInvoice.builder()
                .invoiceNumber(documentSequenceService.generate("INV"))
                .supplierInvoiceRef(request.getSupplierInvoiceRef())
                .purchaseOrder(po)
                .invoiceDate(request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDateTime.now())
                .currency(request.getCurrency() != null ? request.getCurrency() : po.getCurrency())
                .remarks(request.getRemarks())
                .build();

        List<VendorInvoiceLineItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean anyVariance = false;

        for (VendorInvoiceLineItemDTO dto : request.getItems()) {
            BigDecimal lineTotal = dto.getInvoicedUnitPrice().multiply(BigDecimal.valueOf(dto.getInvoicedQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            POLineItem poLine = po.getItems().stream()
                    .filter(l -> l.getSkuCode().equals(dto.getSkuCode()))
                    .findFirst().orElse(null);

            VendorInvoiceLineItem.VendorInvoiceLineItemBuilder itemBuilder = VendorInvoiceLineItem.builder()
                    .vendorInvoice(invoice)
                    .skuCode(dto.getSkuCode())
                    .invoicedQuantity(dto.getInvoicedQuantity())
                    .invoicedUnitPrice(dto.getInvoicedUnitPrice())
                    .invoicedTotal(lineTotal);

            if (poLine == null) {
                // Invoicing for something that was never on this PO at all — always a hold.
                itemBuilder.lineStatus(VendorInvoiceLineItem.LineMatchStatus.UNMATCHED_SKU);
                anyVariance = true;
            } else {
                int acceptedQty = poLine.getAcceptedQuantity() != null ? poLine.getAcceptedQuantity() : 0;
                BigDecimal poPrice = poLine.getUnitPrice();
                int quantityVariance = dto.getInvoicedQuantity() - acceptedQty;

                BigDecimal priceVariancePercent;
                if (poPrice != null && poPrice.compareTo(BigDecimal.ZERO) != 0) {
                    priceVariancePercent = dto.getInvoicedUnitPrice().subtract(poPrice)
                            .divide(poPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                } else {
                    priceVariancePercent = dto.getInvoicedUnitPrice().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO : BigDecimal.valueOf(100);
                }

                // Quantity is zero-tolerance — you cannot invoice more than QA actually
                // accepted. Price has a configurable tolerance band (org setting).
                boolean isVariance = quantityVariance != 0
                        || priceVariancePercent.abs().compareTo(priceTolerancePercent) > 0;
                if (isVariance) anyVariance = true;

                itemBuilder
                        .matchedPoQuantity(acceptedQty)
                        .matchedPoUnitPrice(poPrice)
                        .quantityVariance(quantityVariance)
                        .priceVariancePercent(priceVariancePercent)
                        .lineStatus(isVariance ? VendorInvoiceLineItem.LineMatchStatus.VARIANCE
                                : VendorInvoiceLineItem.LineMatchStatus.MATCHED);
            }
            items.add(itemBuilder.build());
        }

        invoice.setItems(items);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(anyVariance ? InvoiceStatus.VARIANCE_HOLD : InvoiceStatus.MATCHED);

        VendorInvoice saved = invoiceRepository.save(invoice);

        // Opens the approval chain (if any bands are configured for VENDOR_INVOICE) so
        // approve()/reject() below have a chain to work against. If no bands exist yet,
        // this is a no-op — invoices keep behaving exactly as before (single fixed approve).
        approvalEngine.submitForApproval(MODULE, saved.getId(), totalAmount, 1);

        log.info("[VENDOR-INVOICE] {} matched against PO {} — status {} ({} line(s))",
                saved.getInvoiceNumber(), po.getPoNumber(), saved.getStatus(), items.size());
        return mapper.toDTO(saved);
    }

    @Override
    public List<VendorInvoiceResponse> getAll() {
        return invoiceRepository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public VendorInvoiceResponse getById(Long id) {
        return mapper.toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public VendorInvoiceResponse approve(Long id, InvoiceActionRequest request, String approverUsername, Set<String> approverRoles) {
        VendorInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.MATCHED && invoice.getStatus() != InvoiceStatus.VARIANCE_HOLD) {
            throw new IllegalStateException("Only a MATCHED or VARIANCE_HOLD invoice can be approved (current: " + invoice.getStatus() + ").");
        }
        if (invoice.getStatus() == InvoiceStatus.VARIANCE_HOLD
                && (request.getRemarks() == null || request.getRemarks().isBlank())) {
            throw new IllegalArgumentException("An override reason is required to approve an invoice on variance hold.");
        }

        // No bands configured for VENDOR_INVOICE yet → no chain was ever opened → behave
        // exactly like before (single, direct approve). Only consult the engine when a
        // real chain exists, otherwise approve() would fail with "no pending approval".
        boolean hasChain = !approvalEngine.getHistory(MODULE, id).isEmpty();
        if (hasChain) {
            ApprovalOutcome outcome = approvalEngine.approve(MODULE, id, approverUsername, approverRoles, request.getRemarks());
            if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
                invoice.setRemarks(request.getRemarks());
            }
            if (outcome.status() != ApprovalOutcome.Status.FULLY_APPROVED) {
                // Still awaiting the next level — invoice stays MATCHED/VARIANCE_HOLD until then.
                return mapper.toDTO(invoiceRepository.save(invoice));
            }
        }

        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setApprovedAt(LocalDateTime.now());
        invoice.setApprovedBy(approverUsername);
        return mapper.toDTO(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public VendorInvoiceResponse reject(Long id, InvoiceActionRequest request, String approverUsername, Set<String> approverRoles) {
        VendorInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() == InvoiceStatus.APPROVED || invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot reject an invoice that is already " + invoice.getStatus() + ".");
        }
        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new IllegalArgumentException("Rejection remarks are mandatory.");
        }

        boolean hasChain = !approvalEngine.getHistory(MODULE, id).isEmpty();
        if (hasChain) {
            approvalEngine.reject(MODULE, id, approverUsername, approverRoles, request.getRemarks());
        }

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setRemarks(request.getRemarks());
        invoice.setApprovedBy(approverUsername); // tracks who actioned it, approve or reject
        return mapper.toDTO(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public VendorInvoiceResponse markPaid(Long id) {
        VendorInvoice invoice = findOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.APPROVED) {
            throw new IllegalStateException("Only an APPROVED invoice can be marked paid (current: " + invoice.getStatus() + ").");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return mapper.toDTO(invoiceRepository.save(invoice));
    }

    private VendorInvoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor Invoice not found with id " + id));
    }
}
