package com.aerospace.finance_service.service.impl;

import com.aerospace.finance_service.dto.PostingRuleDTO;
import com.aerospace.finance_service.entity.PostingRule;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.PostingRuleRepository;
import com.aerospace.finance_service.service.PostingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostingRuleServiceImpl implements PostingRuleService {

    private final PostingRuleRepository postingRuleRepository;

    @Override
    @Transactional
    public PostingRuleDTO create(PostingRuleDTO dto) {
        PostingRule entity = PostingRule.builder()
                .triggerEvent(dto.getTriggerEvent())
                .debitAccountCode(dto.getDebitAccountCode())
                .creditAccountCode(dto.getCreditAccountCode())
                .taxRuleCode(dto.getTaxRuleCode())
                .costCenterId(dto.getCostCenterId())
                .profitCenterId(dto.getProfitCenterId())
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .build();
        return toDTO(postingRuleRepository.save(entity));
    }

    @Override
    public List<PostingRuleDTO> getAll() {
        return postingRuleRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public PostingRuleDTO update(Long id, PostingRuleDTO dto) {
        PostingRule entity = postingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Posting rule not found: " + id));
        entity.setDebitAccountCode(dto.getDebitAccountCode());
        entity.setCreditAccountCode(dto.getCreditAccountCode());
        entity.setTaxRuleCode(dto.getTaxRuleCode());
        entity.setCostCenterId(dto.getCostCenterId());
        entity.setProfitCenterId(dto.getProfitCenterId());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        return toDTO(postingRuleRepository.save(entity));
    }

    private PostingRuleDTO toDTO(PostingRule e) {
        return PostingRuleDTO.builder()
                .id(e.getId()).triggerEvent(e.getTriggerEvent())
                .debitAccountCode(e.getDebitAccountCode()).creditAccountCode(e.getCreditAccountCode())
                .taxRuleCode(e.getTaxRuleCode()).costCenterId(e.getCostCenterId()).profitCenterId(e.getProfitCenterId())
                .isActive(e.getIsActive())
                .build();
    }
}
