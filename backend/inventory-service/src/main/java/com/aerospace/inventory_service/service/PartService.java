package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.*;
import com.aerospace.inventory_service.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PartService {
    // Basic CRUD
    PartResponseDTO createPart(PartRequestDTO dto);
    List<PartResponseDTO> getAllParts();
    PartResponseDTO getById(Long id);
    PartResponseDTO getBySkuCode(String skuCode);
    PartResponseDTO updatePart(Long id, PartRequestDTO dto);
    void deletePart(Long id);

    // Standard ERP Core Operations (Step 1+2: GRN Inward)
    PartResponseDTO addStock(Long id, int quantity, String batchNumber, String location,
                             LocalDateTime expiryDate, String grnNumber, String challanNumber,
                             String supplierName, String transportName, Integer inspectionQty,
                             String poNumber);
    PartResponseDTO issueToProduction(Long id, int quantity, String batchNumber,
                                       Long workOrderId, String woNumber, Long lineItemId);
    PartResponseDTO processQADecision(Long id, String batchNumber, String newStatus, String remarks);

    // Advanced ERP Integration Methods
    void reserveStock(Long partId, String batch, int quantity);
    PartResponseDTO addStockWithConversion(Long id, int quantity, String inputUom,
                                           String batchNumber, String location);
    void transferStock(Long id, String batch, String fromBin, String toBin, int qty);

    // Search & History
    List<PartResponseDTO> searchByName(String name);
    Page<PartResponseDTO> getAllParts(Pageable pageable);
    List<InventoryTransaction> getTransactionHistory(Long partId); // delegates to TransactionService internally
}