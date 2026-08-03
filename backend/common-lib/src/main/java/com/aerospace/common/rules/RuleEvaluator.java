package com.aerospace.common.rules;

import java.util.Arrays;
import java.util.Map;

// Stateless, no DB — mirrors SlaEvaluator's shape. "Here's a rule, here are the facts
// about this document, tell me if it applies" — nothing more.
public class RuleEvaluator {

    private RuleEvaluator() {}

    public static boolean matches(Rule rule, Map<String, Object> facts) {
        Object actual = facts.get(rule.getFieldName());
        String actualStr = actual == null ? null : String.valueOf(actual);
        return switch (rule.getOperator()) {
            case EQUALS -> rule.getValue().equalsIgnoreCase(actualStr);
            case NOT_EQUALS -> !rule.getValue().equalsIgnoreCase(actualStr);
            case IN -> actualStr != null && Arrays.stream(rule.getValue().split(","))
                    .map(String::trim).anyMatch(v -> v.equalsIgnoreCase(actualStr));
        };
    }
}
