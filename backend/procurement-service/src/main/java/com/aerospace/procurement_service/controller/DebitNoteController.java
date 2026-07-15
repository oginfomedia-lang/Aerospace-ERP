package com.aerospace.procurement_service.controller;

import com.aerospace.procurement_service.dto.DebitNoteResponse;
import com.aerospace.procurement_service.service.DebitNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/debit-notes")
@RequiredArgsConstructor
public class DebitNoteController {
    
    private final DebitNoteService debitNoteService;

    @GetMapping
    public ResponseEntity<List<DebitNoteResponse>> getAll() {
        return ResponseEntity.ok(debitNoteService.getAll());
    }

    @PostMapping("/{id}/mark-settled")
    public ResponseEntity<DebitNoteResponse> markSettled(@PathVariable Long id) {
        return ResponseEntity.ok(debitNoteService.markSettled(id));
    }
}
