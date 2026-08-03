package com.aerospace.common.rules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<Rule, Long> {
    List<Rule> findByModuleCodeAndIsActiveTrue(String moduleCode);
    List<Rule> findAllByOrderByModuleCodeAsc();
}
