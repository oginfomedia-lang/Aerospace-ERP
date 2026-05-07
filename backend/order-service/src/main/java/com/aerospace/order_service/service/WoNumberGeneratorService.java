package com.aerospace.order_service.service;

import com.aerospace.order_service.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique WO numbers in format: WO-2024-00142
 * Thread-safe via AtomicLong sequence.
 */
@Service
@RequiredArgsConstructor
public class WoNumberGeneratorService {

    private final WorkOrderRepository workOrderRepository;
    private final AtomicLong sequence = new AtomicLong(0);

    public synchronized String generate() {
        int year = LocalDate.now().getYear();
        long count = workOrderRepository.count() + sequence.incrementAndGet();
        return String.format("WO-%d-%05d", year, count);
    }
}