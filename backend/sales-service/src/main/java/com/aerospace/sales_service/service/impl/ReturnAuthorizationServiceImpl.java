package com.aerospace.sales_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.sales_service.client.SalesResourceClient;
import com.aerospace.sales_service.client.dto.PartStockInfo;
import com.aerospace.sales_service.dto.ReturnAuthorizationRequestDTO;
import com.aerospace.sales_service.dto.ReturnAuthorizationResponseDTO;
import com.aerospace.sales_service.entity.ReturnAuthorization;
import com.aerospace.sales_service.entity.ReturnDisposition;
import com.aerospace.sales_service.entity.SalesOrder;
import com.aerospace.sales_service.exception.ResourceNotFoundException;
import com.aerospace.sales_service.repository.ReturnAuthorizationRepository;
import com.aerospace.sales_service.repository.SalesOrderRepository;
import com.aerospace.sales_service.service.ReturnAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnAuthorizationServiceImpl implements ReturnAuthorizationService {

    private final ReturnAuthorizationRepository rmaRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final DocumentSequenceService documentSequenceService;
    private final SalesResourceClient salesResourceClient;

    @Override
    @Transactional
    public ReturnAuthorizationResponseDTO authorizeReturn(ReturnAuthorizationRequestDTO dto, String requestedBy) {
        SalesOrder order = salesOrderRepository.findById(dto.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with ID: " + dto.getSalesOrderId()));

        ReturnAuthorization rma = ReturnAuthorization.builder()
                .rmaNumber(documentSequenceService.generate("RMA"))
                .salesOrder(order)
                .partId(dto.getPartId()).partNumber(dto.getPartNumber())
                .quantity(dto.getQuantity())
                .reasonCode(dto.getReasonCode()).reasonDescription(dto.getReasonDescription())
                .requestedBy(requestedBy)
                .disposition(ReturnDisposition.PENDING_INSPECTION)
                .build();
        ReturnAuthorization saved = rmaRepository.save(rma);
        log.info("[RMA] Authorized — rmaNumber={}, orderNumber={}, partNumber={}, qty={}",
                saved.getRmaNumber(), order.getOrderNumber(), saved.getPartNumber(), saved.getQuantity());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ReturnAuthorizationResponseDTO receiveReturn(Long rmaId, String location) {
        ReturnAuthorization rma = findOrThrow(rmaId);
        if (rma.getReceivedBatchNumber() != null) {
            throw new IllegalStateException("This return has already been received (batch " + rma.getReceivedBatchNumber() + ")");
        }
        PartStockInfo stock = salesResourceClient.receiveReturnStock(
                rma.getPartId(), rma.getQuantity(), location, "RETURN-" + rma.getRmaNumber());
        rma.setReceivedBatchNumber(stock.getBatchNumber());
        log.info("[RMA] Received — rmaNumber={}, batch={}, landedStatus={}",
                rma.getRmaNumber(), stock.getBatchNumber(), stock.getStatus());
        return toDTO(rmaRepository.save(rma));
    }

    @Override
    @Transactional
    public ReturnAuthorizationResponseDTO inspectAndDispose(Long rmaId, ReturnDisposition disposition, String inspectedBy, String notes) {
        ReturnAuthorization rma = findOrThrow(rmaId);
        if (rma.getDisposition() != ReturnDisposition.PENDING_INSPECTION) {
            throw new IllegalStateException("This return has already been inspected (current: " + rma.getDisposition() + ")");
        }
        if (rma.getReceivedBatchNumber() == null) {
            throw new IllegalStateException("This return must be received before it can be inspected");
        }

        // Disposition decides what happens to the physical stock — the system doesn't
        // guess, a person decides, and this just carries out whatever they decided.
        switch (disposition) {
            case ACCEPTED -> salesResourceClient.approveReturnedStock(rma.getPartId(), rma.getReceivedBatchNumber(), notes);
            case REJECTED -> salesResourceClient.rejectReturnedStock(rma.getPartId(), rma.getReceivedBatchNumber(), notes);
            case REPAIR, REPLACE, CREDIT_RECOMMENDED -> {
                // No stock-status change here — REPAIR/REPLACE are separate downstream
                // workflows (a new Work Order, a new outbound Dispatch) outside this
                // pass's scope; CREDIT_RECOMMENDED is a flag for a not-yet-built Finance module.
            }
            case PENDING_INSPECTION -> throw new IllegalArgumentException("PENDING_INSPECTION is not a valid inspection outcome");
        }

        rma.setDisposition(disposition);
        rma.setInspectedBy(inspectedBy);
        rma.setInspectedAt(java.time.LocalDateTime.now());
        rma.setInspectionNotes(notes);
        log.info("[RMA] Inspected — rmaNumber={}, disposition={}", rma.getRmaNumber(), disposition);
        return toDTO(rmaRepository.save(rma));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnAuthorizationResponseDTO> listReturns(String disposition) {
        List<ReturnAuthorization> rmas = disposition == null
                ? rmaRepository.findAll()
                : rmaRepository.findByDisposition(ReturnDisposition.valueOf(disposition));
        return rmas.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnAuthorizationResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    private ReturnAuthorization findOrThrow(Long id) {
        return rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return authorization not found with ID: " + id));
    }

    private ReturnAuthorizationResponseDTO toDTO(ReturnAuthorization r) {
        return ReturnAuthorizationResponseDTO.builder()
                .id(r.getId())
                .rmaNumber(r.getRmaNumber())
                .salesOrderId(r.getSalesOrder().getId())
                .orderNumber(r.getSalesOrder().getOrderNumber())
                .partId(r.getPartId())
                .partNumber(r.getPartNumber())
                .quantity(r.getQuantity())
                .reasonCode(r.getReasonCode())
                .reasonDescription(r.getReasonDescription())
                .disposition(r.getDisposition())
                .receivedBatchNumber(r.getReceivedBatchNumber())
                .inspectedBy(r.getInspectedBy())
                .inspectedAt(r.getInspectedAt())
                .inspectionNotes(r.getInspectionNotes())
                .requestedBy(r.getRequestedBy())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
