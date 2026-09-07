package com.aerospace.procurement_service.exception;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> error= new HashMap<>();
        error.put("error", "Not Found");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateResourceException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Duplicate Resource");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(409).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Forbidden");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(403).body(error);
    }

    // Business-rule / validation failures (PUR-BR-*, ARC-BR-*) → 400, not 500
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleValidation(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Validation Error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    // Catch-all for anything not already matched by a more specific handler above (Spring
    // dispatches to the most specific match, so IllegalArgumentException/IllegalStateException
    // above still return their own message — this one is the fallback for genuinely
    // unexpected failures, e.g. a raw SQL/NPE). The real exception is logged server-side;
    // the client only gets a generic message so internals (queries, stack traces, field
    // names) never leak in a 500 response.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred. Please try again or contact support.");
        return ResponseEntity.status(500).body(error);
    }
}
