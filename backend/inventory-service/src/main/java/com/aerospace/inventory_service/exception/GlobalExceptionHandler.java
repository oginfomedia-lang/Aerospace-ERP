package com.aerospace.inventory_service.exception;

import com.aerospace.inventory_service.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // Business-rule / validation failures (ITM-BR-*, INV-BR-*, GRN-FR-*) → 400, not 500
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // Business-state conflicts (e.g. CAPA already open against this NCR) → 409, not 500
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // Catch-all for anything not covered by a more specific handler above — these are
    // NOT the intentional, safe business-rule messages the handlers above return, so the
    // raw exception message (which can carry internal details: SQL, stack internals, class
    // names) must never reach the client. Log it server-side with full detail and return
    // only a generic message in the standard response envelope.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(RuntimeException ex) {
        log.error("Unhandled RuntimeException", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred. Please contact support if the problem persists."));
    }
}