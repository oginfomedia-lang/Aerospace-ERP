package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.LocationRequest;
import com.aerospace.inventory_service.entity.Location;
import com.aerospace.inventory_service.exception.DuplicateResourceException;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.repository.LocationRepository;
import com.aerospace.inventory_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    public Location create(LocationRequest request) {
        if (locationRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Location with code " + request.getCode() + " already exists!");
        }
        Location location = Location.builder()
                .code(request.getCode())
                .name(request.getName())
                .zone(request.getZone())
                .build();
        return locationRepository.save(location);
    }

    @Override
    public Location getById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with ID: " + id));
    }

    @Override
    public List<Location> getAll() {
        return locationRepository.findAll();
    }

    @Override
    public List<Location> getActive() {
        return locationRepository.findByActiveTrue();
    }

    @Override
    public Location update(Long id, LocationRequest request) {
        Location location = getById(id);
        if (!location.getCode().equals(request.getCode())
                && locationRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Location with code " + request.getCode() + " already exists!");
        }
        location.setCode(request.getCode());
        location.setName(request.getName());
        location.setZone(request.getZone());
        return locationRepository.save(location);
    }

    @Override
    public Location setActive(Long id, boolean active) {
        Location location = getById(id);
        location.setActive(active);
        return locationRepository.save(location);
    }

    @Override
    public void assertActive(String code) {
        Location location = locationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unknown location code: " + code + " — add it under Locations first."));
        if (!location.isActive()) {
            throw new IllegalArgumentException(
                    "Location " + code + " has been deactivated and can't accept new stock.");
        }
    }
}
