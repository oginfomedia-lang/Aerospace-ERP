package com.aerospace.finance_service.service;

import com.aerospace.finance_service.dto.PostingRuleDTO;

import java.util.List;

public interface PostingRuleService {
    PostingRuleDTO create(PostingRuleDTO dto);
    List<PostingRuleDTO> getAll();
    PostingRuleDTO update(Long id, PostingRuleDTO dto);
}
