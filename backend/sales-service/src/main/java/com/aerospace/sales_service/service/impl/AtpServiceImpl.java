package com.aerospace.sales_service.service.impl;

import com.aerospace.sales_service.client.SalesResourceClient;
import com.aerospace.sales_service.client.dto.PartStockInfo;
import com.aerospace.sales_service.dto.AtpResultDTO;
import com.aerospace.sales_service.repository.SalesOrderAllocationRepository;
import com.aerospace.sales_service.service.AtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AtpServiceImpl implements AtpService {

    private final SalesResourceClient salesResourceClient;
    private final SalesOrderAllocationRepository allocationRepository;

    @Override
    public AtpResultDTO evaluate(Long salesOrderLineId, Long partId, String partNumber, Integer requestedQuantity) {
        PartStockInfo stock = salesResourceClient.getPartStock(partId);
        int onHand = stock.getQuantity() != null ? stock.getQuantity() : 0;

        // Already promised to OTHER sales orders — this is the piece a naive "just
        // check current stock" ATP would miss, and exactly why §18.8 lists "Existing
        // Customer Commitments" as its own separate input.
        int alreadyCommitted = allocationRepository.sumActiveQuantityByPartId(partId);
        int availableNow = Math.max(0, onHand - alreadyCommitted);

        boolean canFulfillNow = availableNow >= requestedQuantity;
        int shortage = canFulfillNow ? 0 : requestedQuantity - availableNow;

        LocalDate earliest;
        if (canFulfillNow) {
            earliest = LocalDate.now();
        } else {
            // Best-effort estimate: is more of this already planned in production?
            // If yes, assume it lands "soon" (7 days, a simple configurable placeholder);
            // if not, the shortage has no known resolution date yet — flag it further out.
            int plannedProduction = partNumber != null ? salesResourceClient.getPlannedProductionQuantity(partNumber) : 0;
            earliest = plannedProduction >= shortage ? LocalDate.now().plusDays(7) : LocalDate.now().plusDays(30);
        }

        return AtpResultDTO.builder()
                .salesOrderLineId(salesOrderLineId).partId(partId).partNumber(partNumber)
                .requestedQuantity(requestedQuantity)
                .availableNow(availableNow).canFulfillNow(canFulfillNow)
                .earliestDeliveryDate(earliest).shortageQuantity(shortage)
                .build();
    }
}
