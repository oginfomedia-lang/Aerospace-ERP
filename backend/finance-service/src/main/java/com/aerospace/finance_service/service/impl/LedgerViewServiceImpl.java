package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.AgingBucketDTO;
import com.aerospace.finance_service.dto.LedgerViewDTO;
import com.aerospace.finance_service.entity.JournalEntry;
import com.aerospace.finance_service.entity.JournalLine;
import com.aerospace.finance_service.repository.JournalLineRepository;
import com.aerospace.finance_service.service.LedgerViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerViewServiceImpl implements LedgerViewService {

    private final JournalLineRepository journalLineRepository;

    /** "Supplier/Customer Ledger" view: a QUERY over the existing GL, not a separate ledger table. */
    @Override
    public List<LedgerViewDTO> getLedger(String controlAccountCode) {
        List<JournalLine> lines = journalLineRepository.findByAccountCodeAndCounterpartyRefIsNotNullAndJournalEntry_Status(controlAccountCode, JournalEntry.JournalStatus.POSTED);
        Map<String, List<JournalLine>> byCounterparty = lines.stream().collect(Collectors.groupingBy(JournalLine::getCounterpartyRef));

        return byCounterparty.entrySet().stream().map(e -> {
            BigDecimal balance = e.getValue().stream()
                    .map(l -> l.getDebitAmount().subtract(l.getCreditAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return LedgerViewDTO.builder().counterpartyRef(e.getKey()).openBalance(balance)
                    .transactionCount(e.getValue().size()).build();
        }).toList();
    }

    /**
     * Aging: buckets each line by (today - postingDate) into 0-30/31-60/61-90/90+ day
     * buckets. "Unsettled" is approximated by treating every line as contributing its
     * signed amount to whichever bucket its own posting date falls into — the standard
     * simple aging technique when there's no separate open-item/settlement-matching
     * engine (that's future-chapter AP/AR work per the spec's own language).
     */
    @Override
    public List<AgingBucketDTO> getAging(String controlAccountCode, LocalDate asOfDate) {
        List<JournalLine> lines = journalLineRepository.findByAccountCodeAndCounterpartyRefIsNotNullAndJournalEntry_Status(controlAccountCode, JournalEntry.JournalStatus.POSTED);
        Map<String, BigDecimal> b0_30 = new HashMap<>(), b31_60 = new HashMap<>(), b61_90 = new HashMap<>(), b90p = new HashMap<>();

        for (JournalLine line : lines) {
            long daysOld = ChronoUnit.DAYS.between(line.getJournalEntry().getPostingDate(), asOfDate);
            BigDecimal amount = line.getDebitAmount().subtract(line.getCreditAmount());
            Map<String, BigDecimal> target = daysOld <= 30 ? b0_30 : daysOld <= 60 ? b31_60 : daysOld <= 90 ? b61_90 : b90p;
            target.merge(line.getCounterpartyRef(), amount, BigDecimal::add);
        }
        return List.of(toAgingDTO("0-30", b0_30), toAgingDTO("31-60", b31_60), toAgingDTO("61-90", b61_90), toAgingDTO("90+", b90p));
    }

    private AgingBucketDTO toAgingDTO(String label, Map<String, BigDecimal> byCounterparty) {
        BigDecimal total = byCounterparty.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return AgingBucketDTO.builder().bucketLabel(label).totalAmount(total).byCounterparty(byCounterparty).build();
    }
}
