package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.TaxCategoryDTO;
import com.aerospace.finance_service.dto.TaxJurisdictionDTO;
import com.aerospace.finance_service.dto.TaxRateDTO;
import com.aerospace.finance_service.entity.TaxCategory;
import com.aerospace.finance_service.entity.TaxJurisdiction;
import com.aerospace.finance_service.entity.TaxRate;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.TaxCategoryRepository;
import com.aerospace.finance_service.repository.TaxJurisdictionRepository;
import com.aerospace.finance_service.repository.TaxRateRepository;
import com.aerospace.finance_service.service.TaxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxAdminServiceImpl implements TaxAdminService {

    private final TaxCategoryRepository taxCategoryRepository;
    private final TaxJurisdictionRepository taxJurisdictionRepository;
    private final TaxRateRepository taxRateRepository;

    @Override
    @Transactional
    public TaxCategoryDTO createCategory(TaxCategoryDTO dto) {
        TaxCategory entity = TaxCategory.builder()
                .categoryCode(dto.getCategoryCode()).categoryName(dto.getCategoryName())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(taxCategoryRepository.save(entity));
    }

    @Override
    public List<TaxCategoryDTO> getAllCategories() {
        return taxCategoryRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TaxJurisdictionDTO createJurisdiction(TaxJurisdictionDTO dto) {
        TaxJurisdiction entity = TaxJurisdiction.builder()
                .jurisdictionCode(dto.getJurisdictionCode()).jurisdictionName(dto.getJurisdictionName())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(taxJurisdictionRepository.save(entity));
    }

    @Override
    public List<TaxJurisdictionDTO> getAllJurisdictions() {
        return taxJurisdictionRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TaxRateDTO createRate(TaxRateDTO dto) {
        TaxCategory category = dto.getTaxCategoryId() == null ? null :
                taxCategoryRepository.findById(dto.getTaxCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tax category not found: " + dto.getTaxCategoryId()));
        TaxJurisdiction jurisdiction = dto.getTaxJurisdictionId() == null ? null :
                taxJurisdictionRepository.findById(dto.getTaxJurisdictionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tax jurisdiction not found: " + dto.getTaxJurisdictionId()));

        TaxRate entity = TaxRate.builder()
                .taxRuleCode(dto.getTaxRuleCode()).taxCategory(category).taxJurisdiction(jurisdiction)
                .ratePercent(dto.getRatePercent()).direction(dto.getDirection())
                .reverseCharge(dto.getReverseCharge() != null && dto.getReverseCharge())
                .taxAccountCode(dto.getTaxAccountCode())
                .effectiveFrom(dto.getEffectiveFrom()).effectiveTo(dto.getEffectiveTo())
                .activeStatus(dto.getActiveStatus() == null || dto.getActiveStatus())
                .build();
        return toDTO(taxRateRepository.save(entity));
    }

    @Override
    public List<TaxRateDTO> getAllRates() {
        return taxRateRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TaxRateDTO updateRate(Long id, TaxRateDTO dto) {
        TaxRate entity = taxRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rate not found: " + id));
        entity.setRatePercent(dto.getRatePercent());
        entity.setDirection(dto.getDirection());
        if (dto.getReverseCharge() != null) entity.setReverseCharge(dto.getReverseCharge());
        entity.setTaxAccountCode(dto.getTaxAccountCode());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getActiveStatus() != null) entity.setActiveStatus(dto.getActiveStatus());
        return toDTO(taxRateRepository.save(entity));
    }

    private TaxCategoryDTO toDTO(TaxCategory e) {
        return TaxCategoryDTO.builder().id(e.getId()).categoryCode(e.getCategoryCode())
                .categoryName(e.getCategoryName()).activeStatus(e.getActiveStatus()).build();
    }

    private TaxJurisdictionDTO toDTO(TaxJurisdiction e) {
        return TaxJurisdictionDTO.builder().id(e.getId()).jurisdictionCode(e.getJurisdictionCode())
                .jurisdictionName(e.getJurisdictionName()).activeStatus(e.getActiveStatus()).build();
    }

    private TaxRateDTO toDTO(TaxRate e) {
        return TaxRateDTO.builder()
                .id(e.getId()).taxRuleCode(e.getTaxRuleCode())
                .taxCategoryId(e.getTaxCategory() == null ? null : e.getTaxCategory().getId())
                .taxJurisdictionId(e.getTaxJurisdiction() == null ? null : e.getTaxJurisdiction().getId())
                .ratePercent(e.getRatePercent()).direction(e.getDirection()).reverseCharge(e.getReverseCharge())
                .taxAccountCode(e.getTaxAccountCode()).effectiveFrom(e.getEffectiveFrom()).effectiveTo(e.getEffectiveTo())
                .activeStatus(e.getActiveStatus())
                .build();
    }
}
