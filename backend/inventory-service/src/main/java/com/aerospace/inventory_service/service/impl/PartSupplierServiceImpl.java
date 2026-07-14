package com.aerospace.inventory_service.service.impl;

import com.aerospace.inventory_service.dto.PartBasicDTO;
import com.aerospace.inventory_service.dto.PartSupplierRequest;
import com.aerospace.inventory_service.dto.PartSupplierResponse;
import com.aerospace.inventory_service.entity.Part;
import com.aerospace.inventory_service.entity.PartSupplier;
import com.aerospace.inventory_service.exception.ResourceNotFoundException;
import com.aerospace.inventory_service.repository.PartRepository;
import com.aerospace.inventory_service.repository.PartSupplierRepository;
import com.aerospace.inventory_service.service.PartSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartSupplierServiceImpl implements PartSupplierService {

    private final PartSupplierRepository partSupplierRepository;
    private final PartRepository partRepository;

    @Override
    public List<PartSupplierResponse> getApprovedSuppliers(Long partId) {
        return partSupplierRepository.findByPartId(partId).stream()
                .map(ps -> PartSupplierResponse.builder()
                        .supplierId(ps.getSupplierId())
                        .preferred(ps.isPreferred())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public PartSupplierResponse addApprovedSupplier(Long partId, PartSupplierRequest request) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));

        PartSupplier entry = partSupplierRepository.findByPartIdAndSupplierId(partId, request.getSupplierId())
                .orElse(PartSupplier.builder().partId(partId).supplierId(request.getSupplierId()).build());
        entry.setPreferred(request.isPreferred());
        partSupplierRepository.save(entry);

        if (request.isPreferred()) {
            // Only one preferred supplier per part — demote every other approved
            // supplier for this part, and keep Part.preferredSupplierId in sync
            // (still the field the shortage auto-PO logic actually reads).
            partSupplierRepository.findByPartId(partId).forEach(ps -> {
                if (!ps.getSupplierId().equals(request.getSupplierId()) && ps.isPreferred()) {
                    ps.setPreferred(false);
                    partSupplierRepository.save(ps);
                }
            });
            part.setPreferredSupplierId(request.getSupplierId());
            partRepository.save(part);
        }

        return PartSupplierResponse.builder()
                .supplierId(entry.getSupplierId())
                .preferred(entry.isPreferred())
                .build();
    }

    @Override
    @Transactional
    public void removeApprovedSupplier(Long partId, Long supplierId) {
        boolean wasPreferred = partSupplierRepository.findByPartIdAndSupplierId(partId, supplierId)
                .map(PartSupplier::isPreferred).orElse(false);
        partSupplierRepository.deleteByPartIdAndSupplierId(partId, supplierId);

        if (wasPreferred) {
            Part part = partRepository.findById(partId).orElse(null);
            if (part != null && supplierId.equals(part.getPreferredSupplierId())) {
                part.setPreferredSupplierId(null);
                partRepository.save(part);
            }
        }
    }

    @Override
    public List<PartBasicDTO> getPartsForSupplier(Long supplierId) {
        List<PartSupplier> entries = partSupplierRepository.findBySupplierId(supplierId);
        if (entries.isEmpty()) return List.of();

        Map<Long, Boolean> preferredByPartId = entries.stream()
                .collect(Collectors.toMap(PartSupplier::getPartId, PartSupplier::isPreferred));

        return partRepository.findAllById(preferredByPartId.keySet()).stream()
                .map(p -> PartBasicDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .skuCode(p.getSkuCode())
                        .preferred(Boolean.TRUE.equals(preferredByPartId.get(p.getId())))
                        .build())
                .toList();
    }
}
