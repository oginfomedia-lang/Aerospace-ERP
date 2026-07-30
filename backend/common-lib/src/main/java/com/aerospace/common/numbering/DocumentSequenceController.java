package com.aerospace.common.numbering;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-sequence")
@RequiredArgsConstructor
public class DocumentSequenceController {

    private final DocumentSequenceService service;

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<DocumentSequence>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @PutMapping("/{docType}")
    public ResponseEntity<DocumentSequence> updateConfig(@PathVariable String docType,
            @RequestBody Map<String, Object> body) {
        String prefix = (String) body.get("prefix");
        String suffix = (String) body.get("suffix");
        Integer padding = body.get("padding") != null ? Integer.valueOf(body.get("padding").toString()) : null;
        return ResponseEntity.ok(service.updateConfig(docType, prefix, suffix, padding));
    }
}