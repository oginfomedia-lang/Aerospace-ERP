package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.entity.DocumentSequence;
import com.aerospace.inventory_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Chapter 5 §5.7 — configurable document numbering. Only prefix/suffix/padding are
// editable here; nextNumber is never exposed for editing (would break uniqueness).
@RestController
@RequestMapping("/api/v1/inventory/document-sequences")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class DocumentSequenceController {

    private final DocumentSequenceService documentSequenceService;

    @GetMapping
    public ResponseEntity<List<DocumentSequence>> getAll() {
        return ResponseEntity.ok(documentSequenceService.getAll());
    }

    @PutMapping("/{docType}")
    public ResponseEntity<DocumentSequence> updateConfig(
            @PathVariable String docType,
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String suffix,
            @RequestParam(required = false) Integer padding) {
        return ResponseEntity.ok(documentSequenceService.updateConfig(docType, prefix, suffix, padding));
    }
}