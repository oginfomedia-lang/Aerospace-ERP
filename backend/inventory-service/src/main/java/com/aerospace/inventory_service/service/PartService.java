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
    // GRN-FR-004: batchNumber is accepted for signature/backward-compat only and always
    // ignored — the batch number is generated internally (system-assigned, like grnNumber)
    // so every receipt, whatever its source, becomes its own distinct, uniformly-formatted lot.
    PartResponseDTO addStock(Long id, int quantity, String batchNumber, String location,
                             LocalDateTime expiryDate, String grnNumber, String challanNumber,
                             String supplierName, String transportName, Integer inspectionQty,
                             String poNumber);
    PartResponseDTO issueToProduction(Long id, int quantity, String batchNumber,
                                       Long workOrderId, String woNumber, Long lineItemId);

    // Explicit multi-batch allocation — caller specifies exactly which batches and how
    // much from each (e.g. a picker form showing real availability), instead of the
    // server computing a FIFO split.
    PartResponseDTO issueToProductionWithAllocation(Long id, List<BatchAllocationRequest> allocations,
                                                     Long workOrderId, String woNumber, Long lineItemId,
                                                     String issuedTo, String issuedBy);
    // Splits the batch's pending quantity between APPROVED and REJECTED stock — either side
    // can be 0 for the old all-or-nothing behavior, or both > 0 for a genuine partial split.
    PartResponseDTO processQADecision(Long id, String batchNumber, Integer approvedQty, Integer rejectedQty,
                                       String criteria, String remarks, java.util.Set<String> actorRoles);

    // Convenience overload for callers that only have a single status (e.g. the manual
    // admin override endpoint) — resolves the full pending quantity to one side.
    PartResponseDTO processQADecision(Long id, String batchNumber, String newStatus, String remarks, java.util.Set<String> actorRoles);

    // Advanced ERP Integration Methods
    void reserveStock(Long partId, String batch, int quantity);
    PartResponseDTO addStockWithConversion(Long id, int quantity, String inputUom,
                                           String batchNumber, String location);
    void transferStock(Long id, String batch, String fromBin, String toBin, int qty);
    //reverses reserveStock - moves quantity from 'reserved' to 'approved'
    void releaseReservation(Long partId, String batch, int quantity);

    // The physical goods actually leave the building (Sales Order dispatch confirmed) —
    // unlike reserveStock/releaseReservation, this doesn't move quantity to another
    // status bucket; it permanently removes it from the ledger, the same real-world
    // event issueToProduction represents for APPROVED stock, just starting from RESERVED.
    void consumeReservedStock(Long partId, String batch, int quantity);

    // Search & History
    List<PartResponseDTO> searchByName(String name);
    Page<PartResponseDTO> getAllParts(Pageable pageable);
    List<InventoryTransaction> getTransactionHistory(Long partId); // delegates to TransactionService internally\

    ImportResultDTO importParts(List<PartImportRowDTO> rows);
}