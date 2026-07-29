package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.entity.DocumentSequence;
import com.aerospace.procurement_service.service.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;;


@RestController
@RequestMapping("/api/document-sequences")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
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
