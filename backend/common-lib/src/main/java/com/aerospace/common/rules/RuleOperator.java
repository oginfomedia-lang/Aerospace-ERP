package com.aerospace.common.rules;

// Deliberately tiny — only what the one concrete v1 use case (criticality gating)
// needs, matching SlaEvaluator's "small utility, not a framework" philosophy.
public enum RuleOperator { EQUALS, NOT_EQUALS, IN }
