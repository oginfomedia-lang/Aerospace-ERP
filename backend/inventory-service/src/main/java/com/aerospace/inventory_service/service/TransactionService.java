package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.entity.InventoryTransaction;
import java.util.List;

public interface TransactionService {

    // Used internally by PartServiceImpl to record every stock movement.
    // referenceDoc carries the source document number (GRN#, WO#, transfer ref, etc.)
    // per the generic reference pattern (FUT-BR-003) — pass null when not applicable.
    void logTransaction(Long partId, String batchNumber, String transactionType,
                        int quantity, String referenceDoc, String remarks);

    // Used by PartController for audit trail by part
    List<InventoryTransaction> getByPartId(Long partId);

    // Used by PartController for batch traceability — auditors trace a batch end-to-end
    List<InventoryTransaction> getByBatchNumber(String batchNumber);

    // Used by PartController for Finance/reporting — e.g. all GRN_RECEIPT this month
    List<InventoryTransaction> getByTransactionType(String transactionType);
}