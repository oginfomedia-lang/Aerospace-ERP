package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.entity.InventoryTransaction;
import com.aerospace.inventory_service.repository.TransactionRepository;
import com.aerospace.inventory_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public void logTransaction(Long partId, String batchNumber, String transactionType,
                               int quantity, String referenceDoc, String remarks) {
        transactionRepository.save(InventoryTransaction.builder()
                .partId(partId)
                .batchNumber(batchNumber)
                .transactionType(transactionType)
                .quantity(quantity)
                .referenceDoc(referenceDoc)
                .remarks(remarks)
                .build());
    }

    @Override
    public List<InventoryTransaction> getByPartId(Long partId) {
        return transactionRepository.findByPartIdOrderByTimestampDesc(partId);
    }

    @Override
    public List<InventoryTransaction> getByBatchNumber(String batchNumber) {
        return transactionRepository.findByBatchNumberOrderByTimestampDesc(batchNumber);
    }

    @Override
    public List<InventoryTransaction> getByTransactionType(String transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }
}