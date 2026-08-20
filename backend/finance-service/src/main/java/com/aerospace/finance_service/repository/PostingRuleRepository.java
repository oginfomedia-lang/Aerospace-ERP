package com.aerospace.finance_service.repository;

import com.aerospace.finance_service.entity.PostingRule;
import com.aerospace.finance_service.entity.PostingTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostingRuleRepository extends JpaRepository<PostingRule, Long> {
    Optional<PostingRule> findByTriggerEventAndIsActiveTrue(PostingTriggerType triggerEvent);
}
