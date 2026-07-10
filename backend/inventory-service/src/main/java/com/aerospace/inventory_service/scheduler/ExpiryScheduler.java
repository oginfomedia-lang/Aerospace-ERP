package com.aerospace.inventory_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aerospace.inventory_service.entity.StockBalance;
import com.aerospace.inventory_service.entity.StockStatus;
import com.aerospace.inventory_service.repository.StockBalanceRepository;
import com.aerospace.inventory_service.service.TransactionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryScheduler {
    private final StockBalanceRepository balanceRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 2 * * *") //runs every day at 2AM
    @Transactional
    public void autoRejectExpiredStock(){
        List<StockBalance> expired = balanceRepository.findAllByExpiryDateBeforeAndStatus(LocalDateTime.now(), StockStatus.APPROVED);

        for(StockBalance balance: expired) {
            log.warn("Auto-rejecting expired batch: {} for partid: {}", balance.getBatchNumber(), balance.getPartId());

            balance.setStatus(StockStatus.REJECTED);
            balanceRepository.save(balance);

            transactionService.logTransaction(
                balance.getPartId(),
                balance.getBatchNumber(),
                "EXPIRY_REJECTION",
                0,
                null,
                "Auto-rejected by scheduler - batch expired on " + balance.getExpiryDate()
                );
        }
        log.info("Expiry Scheduler completed. {} batches auto-rejected.", expired.size());
    }
}
