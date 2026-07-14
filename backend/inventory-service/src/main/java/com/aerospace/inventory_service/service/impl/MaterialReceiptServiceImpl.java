package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.FormGrnRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptRequest;
import com.aerospace.inventory_service.dto.MaterialReceiptResponse;
import com.aerospace.inventory_service.dto.PartResponseDTO;
import com.aerospace.inventory_service.entity.MaterialReceipt;
import com.aerospace.inventory_service.entity.MaterialReceiptItem;
import com.aerospace.inventory_service.entity.MaterialReceiptStatus;
import com.aerospace.inventory_service.entity.Part;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.mapper.MaterialReceiptMapper;
import com.aerospace.inventory_service.repository.MaterialReceiptRepository;
import com.aerospace.inventory_service.repository.PartRepository;
import com.aerospace.inventory_service.service.DocumentSequenceService;
import com.aerospace.inventory_service.service.MaterialReceiptService;
import com.aerospace.inventory_service.service.PartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialReceiptServiceImpl implements MaterialReceiptService {

    private final MaterialReceiptRepository receiptRepository;
    private final PartRepository partRepository;
    private final PartService partService;
    private final DocumentSequenceService documentSequenceService;
    private final MaterialReceiptMapper mapper;

    @Override
    @Transactional
    public MaterialReceiptResponse createReceipt(MaterialReceiptRequest request) {
        String mrNumber = documentSequenceService.generate("MR");

        MaterialReceipt receipt = MaterialReceipt.builder()
                .mrNumber(mrNumber)
                .poNumber(request.getPoNumber())
                .supplierName(request.getSupplierName())
                .challanNumber(request.getChallanNumber())
                .transportName(request.getTransportName())
                .receivedBy(request.getReceivedBy())
                .status(MaterialReceiptStatus.RECEIVED_AT_GATE)
                .build();

        List<MaterialReceiptItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            request.getItems().forEach(i -> items.add(MaterialReceiptItem.builder()
                    .materialReceipt(receipt)
                    .skuCode(i.getSkuCode())
                    .quantity(i.getQuantity())
                    .uom(i.getUom())
                    .build()));
        }
        receipt.setItems(items);

        MaterialReceipt saved = receiptRepository.save(receipt);
        log.info("[MATERIAL-RECEIPT] {} recorded at gate for PO {}", mrNumber, request.getPoNumber());
        return mapper.toDTO(saved);
    }

    @Override
    public List<MaterialReceiptResponse> getAll(String status) {
        List<MaterialReceipt> receipts = (status != null && !status.isBlank())
                ? receiptRepository.findByStatus(MaterialReceiptStatus.valueOf(status.toUpperCase()))
                : receiptRepository.findAll();
        return receipts.stream().map(mapper::toDTO).toList();
    }

    @Override
    public MaterialReceiptResponse getById(Long id) {
        return mapper.toDTO(findOrThrow(id));
    }

    // GRN is only ever formed from a MaterialReceipt that hasn't been processed yet —
    // this is what enforces "gate receipt before GRN" at the API level, not just the UI.
    @Override
    @Transactional
    public MaterialReceiptResponse formGrn(Long id, FormGrnRequest request) {
        MaterialReceipt receipt = findOrThrow(id);
        if (receipt.getStatus() == MaterialReceiptStatus.GRN_FORMED) {
            throw new IllegalStateException("GRN has already been formed from Material Receipt " + receipt.getMrNumber() + ".");
        }

        // Batch defaults to the PO number, same convention the old direct-from-PO GRN form used.
        String defaultBatch = receipt.getPoNumber() != null ? receipt.getPoNumber() : receipt.getMrNumber();

        for (MaterialReceiptItem item : receipt.getItems()) {
            Part part = partRepository.findBySkuCode(item.getSkuCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "SKU " + item.getSkuCode() + " on receipt " + receipt.getMrNumber()
                                    + " does not match any inventory item."));

            PartResponseDTO result = partService.addStock(
                    part.getId(),
                    item.getQuantity(),
                    defaultBatch,
                    request.getLocation(),
                    request.getExpiryDate(),
                    null, // GRN-FR-001: always system-generated, never passed in
                    receipt.getChallanNumber(),
                    receipt.getSupplierName(),
                    receipt.getTransportName(),
                    request.getInspectionQty(),
                    receipt.getPoNumber()
            );
            item.setGrnNumber(result.getGrnNumber());
            item.setBatchNumber(defaultBatch);
            item.setLocation(request.getLocation());
            item.setExpiryDate(request.getExpiryDate());
            item.setInspectionQty(request.getInspectionQty());
        }

        receipt.setStatus(MaterialReceiptStatus.GRN_FORMED);
        receipt.setGrnFormedAt(java.time.LocalDateTime.now());
        MaterialReceipt saved = receiptRepository.save(receipt);
        log.info("[MATERIAL-RECEIPT] GRN formed from {} ({} item(s))", receipt.getMrNumber(), receipt.getItems().size());
        return mapper.toDTO(saved);
    }

    private MaterialReceipt findOrThrow(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material Receipt not found: " + id));
    }
}
