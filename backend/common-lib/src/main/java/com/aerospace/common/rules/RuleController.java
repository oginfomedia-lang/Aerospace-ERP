package com.aerospace.common.rules;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRUD for business rules — same shape as ApprovalMatrixController — so rules are
// configurable without code changes (chapter 7.16 acceptance criterion).
@RestController
@RequestMapping("/api/business-rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleRepository repository;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<Rule>> getAll(@RequestParam(required = false) String moduleCode) {
        List<Rule> result = (moduleCode != null && !moduleCode.isBlank())
                ? repository.findByModuleCodeAndIsActiveTrue(moduleCode)
                : repository.findAllByOrderByModuleCodeAsc();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<Rule> create(@RequestBody Rule rule) {
        rule.setId(null);
        return ResponseEntity.ok(repository.save(rule));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Rule> update(@PathVariable Long id, @RequestBody Rule request) {
        Rule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + id));
        rule.setModuleCode(request.getModuleCode());
        rule.setFieldName(request.getFieldName());
        rule.setOperator(request.getOperator());
        rule.setValue(request.getValue());
        rule.setAction(request.getAction());
        rule.setActionValue(request.getActionValue());
        rule.setActive(request.isActive());
        return ResponseEntity.ok(repository.save(rule));
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
