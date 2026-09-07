package com.aerospace.sales_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.sales_service.dto.QuotationLineRequestDTO;
import com.aerospace.sales_service.dto.QuotationLineResponseDTO;
import com.aerospace.sales_service.dto.QuotationRequestDTO;
import com.aerospace.sales_service.dto.QuotationResponseDTO;
import com.aerospace.sales_service.dto.SalesOrderLineRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderRequestDTO;
import com.aerospace.sales_service.dto.SalesOrderResponseDTO;
import com.aerospace.sales_service.entity.Customer;
import com.aerospace.sales_service.entity.Quotation;
import com.aerospace.sales_service.entity.QuotationLine;
import com.aerospace.sales_service.entity.QuotationStatus;
import com.aerospace.sales_service.exception.ResourceNotFoundException;
import com.aerospace.sales_service.repository.CustomerRepository;
import com.aerospace.sales_service.repository.QuotationRepository;
import com.aerospace.sales_service.service.QuotationService;
import com.aerospace.sales_service.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotationServiceImpl implements QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final DocumentSequenceService documentSequenceService;
    // Same service, same database, no HTTP involved — this is one Spring bean calling
    // another bean's public interface directly (unlike SalesResourceClient later, which
    // is for TRUE cross-microservice calls to a different service/port/database).
    private final SalesOrderService salesOrderService;

    @Override
    @Transactional
    public QuotationResponseDTO createQuotation(QuotationRequestDTO dto, String createdBy) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + dto.getCustomerId()));

        Quotation quotation = Quotation.builder()
                .quotationNumber(documentSequenceService.generate("QUOTE"))
                .customer(customer)
                .validFrom(dto.getValidFrom())
                .validUntil(dto.getValidUntil())
                .currency(dto.getCurrency())
                .paymentTerms(dto.getPaymentTerms())
                .deliveryTerms(dto.getDeliveryTerms())
                .status(QuotationStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        List<QuotationLine> lines = dto.getLines().stream().map(l -> toLineEntity(l, quotation)).toList();
        quotation.setLines(lines);
        quotation.setTotalAmount(sumLines(lines));

        Quotation saved = quotationRepository.save(quotation);
        log.info("Quotation created — quotationNumber: {}, customer: {}, total: {}",
                saved.getQuotationNumber(), customer.getName(), saved.getTotalAmount());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public QuotationResponseDTO updateQuotation(Long id, QuotationRequestDTO dto) {
        Quotation quotation = findOrThrow(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT quotation can be edited (current: " + quotation.getStatus() + ")");
        }
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + dto.getCustomerId()));

        quotation.setCustomer(customer);
        quotation.setValidFrom(dto.getValidFrom());
        quotation.setValidUntil(dto.getValidUntil());
        quotation.setCurrency(dto.getCurrency());
        quotation.setPaymentTerms(dto.getPaymentTerms());
        quotation.setDeliveryTerms(dto.getDeliveryTerms());

        // orphanRemoval=true on Quotation.lines means clear()+addAll() correctly deletes
        // the old rows and inserts fresh ones on save — same pattern used everywhere else
        // in this codebase a parent's child list gets wholesale-replaced on edit.
        quotation.getLines().clear();
        quotation.getLines().addAll(dto.getLines().stream().map(l -> toLineEntity(l, quotation)).toList());
        quotation.setTotalAmount(sumLines(quotation.getLines()));

        return toDTO(quotationRepository.save(quotation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotationResponseDTO> listQuotations(String status) {
        List<Quotation> quotations = status == null
                ? quotationRepository.findAll()
                : quotationRepository.findByStatus(QuotationStatus.valueOf(status));
        return quotations.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationResponseDTO getQuotationById(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public QuotationResponseDTO submitForApproval(Long id) {
        Quotation quotation = findOrThrow(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Only a DRAFT quotation can be submitted (current: " + quotation.getStatus() + ")");
        }
        quotation.setStatus(QuotationStatus.SUBMITTED);
        return toDTO(quotationRepository.save(quotation));
    }

    @Override
    @Transactional
    public QuotationResponseDTO approve(Long id, String approverUsername) {
        Quotation quotation = findOrThrow(id);
        if (quotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw new IllegalStateException("Only a SUBMITTED quotation can be approved (current: " + quotation.getStatus() + ")");
        }
        if (quotation.getCreatedBy() != null && quotation.getCreatedBy().equalsIgnoreCase(approverUsername)) {
            throw new IllegalStateException("You cannot approve a quotation you created yourself.");
        }
        quotation.setStatus(QuotationStatus.APPROVED);
        return toDTO(quotationRepository.save(quotation));
    }

    @Override
    @Transactional
    public QuotationResponseDTO reject(Long id) {
        Quotation quotation = findOrThrow(id);
        if (quotation.getStatus() != QuotationStatus.SUBMITTED) {
            throw new IllegalStateException("Only a SUBMITTED quotation can be rejected (current: " + quotation.getStatus() + ")");
        }
        quotation.setStatus(QuotationStatus.REJECTED);
        return toDTO(quotationRepository.save(quotation));
    }

    // The real "commitment" moment — copies every line off the quotation onto a brand
    // new Sales Order, then stamps the quotation CONVERTED so nobody can convert it a
    // second time. Only an APPROVED quotation is allowed through; everything else (still
    // DRAFT, already REJECTED, already CONVERTED, ...) is rejected with a clear reason.
    @Override
    @Transactional
    public SalesOrderResponseDTO convertToSalesOrder(Long quotationId) {
        Quotation quotation = findOrThrow(quotationId);
        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only an APPROVED quotation can be converted (current: " + quotation.getStatus() + ")");
        }

        SalesOrderRequestDTO orderDto = new SalesOrderRequestDTO();
        orderDto.setCustomerId(quotation.getCustomer().getId());
        orderDto.setOrderType("STANDARD");
        orderDto.setShippingAddress(quotation.getCustomer().getShippingAddress());
        orderDto.setLines(quotation.getLines().stream().map(l -> {
            SalesOrderLineRequestDTO line = new SalesOrderLineRequestDTO();
            line.setPartId(l.getPartId());
            line.setPartNumber(l.getPartNumber());
            line.setPartName(l.getPartName());
            line.setQuantity(l.getQuantity());
            line.setUnitPrice(l.getUnitPrice());
            line.setDiscountPercent(l.getDiscountPercent());
            line.setTaxPercent(l.getTaxPercent());
            return line;
        }).toList());

        SalesOrderResponseDTO created = salesOrderService.createOrder(orderDto, quotation.getCreatedBy());

        quotation.setStatus(QuotationStatus.CONVERTED);
        quotation.setConvertedToSalesOrderId(created.getId());
        quotationRepository.save(quotation);

        log.info("[QUOTATION] Converted — quotationNumber={} -> orderNumber={}",
                quotation.getQuotationNumber(), created.getOrderNumber());
        return created;
    }

    // Never trust a client-sent total — always recompute server-side from the actual line
    // items, using the unit price the server itself has on file. If this math only ever
    // happened in the browser, anyone with dev tools open could rewrite the numbers
    // before hitting Save; recomputing it here closes that door completely.
    private QuotationLine toLineEntity(QuotationLineRequestDTO d, Quotation quotation) {
        BigDecimal discount = d.getDiscountPercent() != null ? d.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal tax = d.getTaxPercent() != null ? d.getTaxPercent() : BigDecimal.ZERO;
        BigDecimal gross = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
        BigDecimal afterDiscount = gross.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100))));
        BigDecimal lineTotal = afterDiscount.multiply(BigDecimal.ONE.add(tax.divide(BigDecimal.valueOf(100))))
                .setScale(2, RoundingMode.HALF_UP);
        return QuotationLine.builder()
                .quotation(quotation).partId(d.getPartId()).partNumber(d.getPartNumber()).partName(d.getPartName())
                .quantity(d.getQuantity()).unitPrice(d.getUnitPrice()).discountPercent(discount).taxPercent(tax)
                .lineTotal(lineTotal)
                .build();
    }

    private BigDecimal sumLines(List<QuotationLine> lines) {
        return lines.stream().map(QuotationLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Quotation findOrThrow(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with ID: " + id));
    }

    private QuotationResponseDTO toDTO(Quotation q) {
        List<QuotationLineResponseDTO> lineDTOs = q.getLines().stream()
                .map(l -> QuotationLineResponseDTO.builder()
                        .id(l.getId()).partId(l.getPartId()).partNumber(l.getPartNumber()).partName(l.getPartName())
                        .quantity(l.getQuantity()).unitPrice(l.getUnitPrice()).discountPercent(l.getDiscountPercent())
                        .taxPercent(l.getTaxPercent()).lineTotal(l.getLineTotal())
                        .build())
                .toList();
        return QuotationResponseDTO.builder()
                .id(q.getId())
                .quotationNumber(q.getQuotationNumber())
                .customerId(q.getCustomer().getId())
                .customerName(q.getCustomer().getName())
                .validFrom(q.getValidFrom())
                .validUntil(q.getValidUntil())
                .currency(q.getCurrency())
                .paymentTerms(q.getPaymentTerms())
                .deliveryTerms(q.getDeliveryTerms())
                .totalAmount(q.getTotalAmount())
                .status(q.getStatus())
                .convertedToSalesOrderId(q.getConvertedToSalesOrderId())
                .lines(lineDTOs)
                .createdBy(q.getCreatedBy())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}