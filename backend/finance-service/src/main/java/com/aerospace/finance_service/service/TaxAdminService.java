package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.TaxCategoryDTO;
import com.aerospace.finance_service.dto.TaxJurisdictionDTO;
import com.aerospace.finance_service.dto.TaxRateDTO;

import java.util.List;

public interface TaxAdminService {
    TaxCategoryDTO createCategory(TaxCategoryDTO dto);
    List<TaxCategoryDTO> getAllCategories();

    TaxJurisdictionDTO createJurisdiction(TaxJurisdictionDTO dto);
    List<TaxJurisdictionDTO> getAllJurisdictions();

    TaxRateDTO createRate(TaxRateDTO dto);
    List<TaxRateDTO> getAllRates();
    TaxRateDTO updateRate(Long id, TaxRateDTO dto);
}
