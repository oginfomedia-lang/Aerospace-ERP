package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.DispositionRequest;
import com.aerospace.inventory_service.dto.NcrResponse;
import com.aerospace.inventory_service.entity.DispositionType;
import com.aerospace.inventory_service.entity.NcrStatus;
import com.aerospace.inventory_service.entity.NonConformanceReport;
import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.mapper.NcrMapper;
import com.aerospace.inventory_service.repository.NcrRepository;
import com.aerospace.inventory_service.repository.StockBalanceRepository;
import com.aerospace.inventory_service.service.NcrService;
import com.aerospace.inventory_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NcrServiceImpl implements NcrService {

    private final NcrRepository ncrRepository;
    private final StockBalanceRepository balanceRepository;
    private final TransactionService transactionService;
    private final NcrMapper mapper;

    @Override
    public List<NcrResponse> getAll(String status) {
        List<NonConformanceReport> ncrs = (status != null && !status.isBlank())
                ? ncrRepository.findByStatus(NcrStatus.valueOf(status.toUpperCase()))
                : ncrRepository.findAll();
        return ncrs.stream().map(mapper::toDTO).toList();
    }

    @Override
    public NcrResponse getById(Long id) {
        return mapper.toDTO(findOrThrow(id));
    }

    @Override
    @Transactional
    public NcrResponse disposition(Long id, DispositionRequest request) {
        NonConformanceReport ncr = findOrThrow(id);
        if (ncr.getStatus() == NcrStatus.DISPOSITIONED) {
            throw new IllegalStateException("NCR " + ncr.getNcrNumber() + " has already been dispositioned.");
        }
        if (request.getDispositionedBy() == null || request.getDispositionedBy().isBlank()) {
            throw new IllegalArgumentException("A disposition approver name is required.");
        }

        DispositionType type;
        try {
            type = DispositionType.valueOf(request.getDispositionType().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown disposition type: " + request.getDispositionType());
        }

        StockBalance rejected = balanceRepository
                .findByPartIdAndBatchNumberAndStatus(ncr.getPartId(), ncr.getBatchNumber(), StockStatus.REJECTED)
                .orElseThrow(() -> new IllegalStateException(
                        "No REJECTED stock found for batch " + ncr.getBatchNumber()
                                + " — it may have already been dispositioned elsewhere."));

        int qty = Math.min(ncr.getQuantity(), rejected.getQuantity());

        switch (type) {
            case RETURN_TO_VENDOR, SCRAP -> {
                // A true exit from the ledger — unlike the bucket-move conventions below,
                // this stock is genuinely leaving (back to vendor, or destroyed).
                rejected.setQuantity(rejected.getQuantity() - qty);
                balanceRepository.save(rejected);
                transactionService.logTransaction(ncr.getPartId(), ncr.getBatchNumber(), "NCR_DISPOSITION",
                        -qty, ncr.getNcrNumber(), type.name() + ": " + request.getRemarks());
            }
            case USE_AS_IS -> {
                rejected.setQuantity(rejected.getQuantity() - qty);
                balanceRepository.save(rejected);

                StockBalance approved = balanceRepository
                        .findByPartIdAndBatchNumberAndStatus(ncr.getPartId(), ncr.getBatchNumber(), StockStatus.APPROVED)
                        .orElse(StockBalance.builder()
                                .partId(ncr.getPartId()).batchNumber(ncr.getBatchNumber())
                                .status(StockStatus.APPROVED).quantity(0)
                                .location(rejected.getLocation()).expiryDate(rejected.getExpiryDate())
                                .build());
                approved.setQuantity(approved.getQuantity() + qty);
                balanceRepository.save(approved);

                // Bucket move, not a net quantity change — log 0, same convention already
                // used for QA_DECISION/STOCK_RESERVATION/LOCATION_TRANSFER.
                transactionService.logTransaction(ncr.getPartId(), ncr.getBatchNumber(), "NCR_DISPOSITION",
                        0, ncr.getNcrNumber(),
                        "USE_AS_IS (special approval by " + request.getDispositionedBy() + "): " + request.getRemarks());
            }
        }

        ncr.setStatus(NcrStatus.DISPOSITIONED);
        ncr.setDispositionType(type);
        ncr.setDispositionRemarks(request.getRemarks());
        ncr.setDispositionedBy(request.getDispositionedBy());
        ncr.setDispositionedAt(LocalDateTime.now());

        NonConformanceReport saved = ncrRepository.save(ncr);
        log.info("[NCR] {} dispositioned as {} by {}", ncr.getNcrNumber(), type, request.getDispositionedBy());
        return mapper.toDTO(saved);
    }

    private NonConformanceReport findOrThrow(Long id) {
        return ncrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NCR not found: " + id));
    }
}
