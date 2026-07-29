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
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found"));
        return partSupplierRepository.findByPartId(partId).stream()
                .map(ps -> PartSupplierResponse.builder()
                        .supplierId(ps.getSupplierId())
                        .preferred(ps.getSupplierId().equals(part.getPreferredSupplierId()))
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
        partSupplierRepository.save(entry);

        if (request.isPreferred()) {
            part.setPreferredSupplierId(request.getSupplierId());
            partRepository.save(part);
        }

        return PartSupplierResponse.builder()
                .supplierId(entry.getSupplierId())
                .preferred(request.isPreferred())
                .build();
    }

    @Override
    @Transactional
    public void removeApprovedSupplier(Long partId, Long supplierId) {
        partSupplierRepository.deleteByPartIdAndSupplierId(partId, supplierId);

        Part part = partRepository.findById(partId).orElse(null);
        if (part != null && supplierId.equals(part.getPreferredSupplierId())) {
            part.setPreferredSupplierId(null);
            partRepository.save(part);
        }
    }

    @Override
    public List<PartBasicDTO> getPartsForSupplier(Long supplierId) {
        List<PartSupplier> entries = partSupplierRepository.findBySupplierId(supplierId);
        if (entries.isEmpty()) return List.of();

        List<Long> partIds = entries.stream().map(PartSupplier::getPartId).toList();
        Map<Long, Part> partsById = partRepository.findAllById(partIds).stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        return entries.stream()
                .map(ps -> {
                    Part p = partsById.get(ps.getPartId());
                    if (p == null) return null;
                    return PartBasicDTO.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .skuCode(p.getSkuCode())
                            .preferred(ps.getSupplierId().equals(p.getPreferredSupplierId()))
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}