package com.aerospace.common.rules;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final RuleRepository ruleRepository;

    // Returns the set of roles required by any matching, active rule for this module+facts.
    // Empty set = no additional gating beyond whatever the caller already enforces.
    public Set<String> getRequiredRoles(String moduleCode, Map<String, Object> facts) {
        return ruleRepository.findByModuleCodeAndIsActiveTrue(moduleCode).stream()
                .filter(r -> RuleEvaluator.matches(r, facts))
                .filter(r -> r.getAction() == RuleAction.REQUIRE_ROLE)
                .map(Rule::getActionValue)
                .collect(Collectors.toSet());
    }
}
