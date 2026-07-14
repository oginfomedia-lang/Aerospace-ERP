package com.aerospace.inventory_service.service;

import com.aerospace.inventory_service.dto.LocationRequest;
import com.aerospace.inventory_service.entity.Location;

import java.util.List;

public interface LocationService {
    Location create(LocationRequest request);
    Location getById(Long id);
    List<Location> getAll();
    List<Location> getActive();
    Location update(Long id, LocationRequest request);
    Location setActive(Long id, boolean active);

    // Used by addStock/formGrn/transferStock to validate a location code before
    // accepting it — throws if the code doesn't exist or has been deactivated.
    void assertActive(String code);
}
