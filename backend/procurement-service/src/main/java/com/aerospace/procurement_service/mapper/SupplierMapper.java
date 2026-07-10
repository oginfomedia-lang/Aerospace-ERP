package com.aerospace.procurement_service.mapper;

import com.aerospace.procurement_service.dto.SupplierRequest;
import com.aerospace.procurement_service.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public Supplier toEntity(SupplierRequest dto) {
        if (dto == null) return null;

        return Supplier.builder()
                .name(dto.getName())
                .vendorCode(dto.getVendorCode())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .certificationStatus(dto.getCertificationStatus())
                .rating(dto.getRating())
                .build();
    }

    // Applies request fields onto an existing entity in place (used by update)
    public void updateEntity(Supplier supplier, SupplierRequest dto) {
        supplier.setName(dto.getName());
        supplier.setVendorCode(dto.getVendorCode());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setCertificationStatus(dto.getCertificationStatus());
        supplier.setRating(dto.getRating());
    }
}
