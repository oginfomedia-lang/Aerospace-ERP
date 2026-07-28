package com.aerospace.inventory_service.controller;

import com.aerospace.inventory_service.dto.LocationRequest;
import com.aerospace.inventory_service.entity.Location;
import com.aerospace.inventory_service.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/inventory/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @PostMapping
    public ResponseEntity<Location> create(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Location>> getAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly ? locationService.getActive() : locationService.getAll());
    }

    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Location> update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationService.update(id, request));
    }

    // Soft delete — deactivates rather than removing, so stock/GRN history stays intact
    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Location> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.setActive(id, false));
    }

    @PreAuthorize("hasAnyRole('STORE_KEEPER','MANAGER','ADMIN')")
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Location> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.setActive(id, true));
    }
}
