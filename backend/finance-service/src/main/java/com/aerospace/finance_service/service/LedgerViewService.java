package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.AgingBucketDTO;
import com.aerospace.finance_service.dto.LedgerViewDTO;

import java.time.LocalDate;
import java.util.List;

public interface LedgerViewService {
    List<LedgerViewDTO> getLedger(String controlAccountCode);
    List<AgingBucketDTO> getAging(String controlAccountCode, LocalDate asOfDate);
}
