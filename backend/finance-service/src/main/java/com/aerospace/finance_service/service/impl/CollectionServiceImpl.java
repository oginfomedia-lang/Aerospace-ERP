package com.aerospace.finance_service.service.impl;

import com.aerospace.common.numbering.DocumentSequenceService;
import com.aerospace.finance_service.dto.CollectionActivityCreateDTO;
import com.aerospace.finance_service.dto.CollectionActivityDTO;
import com.aerospace.finance_service.entity.CollectionActivity;
import com.aerospace.finance_service.entity.CollectionActivity.ActivityStatus;
import com.aerospace.finance_service.exception.ResourceNotFoundException;
import com.aerospace.finance_service.repository.CollectionActivityRepository;
import com.aerospace.finance_service.security.TenantContext;
import com.aerospace.finance_service.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionActivityRepository repository;
    private final DocumentSequenceService documentSequenceService;

    @Override
    @Transactional
    public CollectionActivityDTO logActivity(CollectionActivityCreateDTO dto, String currentUser) {
        CollectionActivity activity = CollectionActivity.builder()
                .tenantId(TenantContext.getTenantId())
                .activityNumber(documentSequenceService.generate("COL"))
                .customerRef(dto.getCustomerRef())
                .invoiceRef(dto.getInvoiceRef())
                .activityDate(dto.getActivityDate())
                .activityType(dto.getActivityType())
                .notes(dto.getNotes())
                .promisedPaymentDate(dto.getPromisedPaymentDate())
                .promisedAmount(dto.getPromisedAmount())
                .followUpDate(dto.getFollowUpDate())
                .status(ActivityStatus.OPEN)
                .createdBy(currentUser)
                .build();
        return toDTO(repository.save(activity));
    }

    @Override
    public List<CollectionActivityDTO> getByCustomer(String customerRef) {
        return repository.findByCustomerRef(customerRef).stream().map(this::toDTO).toList();
    }

    @Override
    public List<CollectionActivityDTO> getAll() {
        String tenantId = TenantContext.getTenantId();
        List<CollectionActivity> records = tenantId != null
                ? repository.findByTenantId(tenantId)
                : repository.findAll();
        return records.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public CollectionActivityDTO resolve(Long id) {
        CollectionActivity a = getOrThrow(id);
        a.setStatus(ActivityStatus.RESOLVED);
        return toDTO(repository.save(a));
    }

    @Override
    @Transactional
    public CollectionActivityDTO escalate(Long id) {
        CollectionActivity a = getOrThrow(id);
        a.setStatus(ActivityStatus.ESCALATED);
        return toDTO(repository.save(a));
    }

    private CollectionActivity getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection activity not found: " + id));
    }

    private CollectionActivityDTO toDTO(CollectionActivity a) {
        return CollectionActivityDTO.builder()
                .id(a.getId()).activityNumber(a.getActivityNumber())
                .customerRef(a.getCustomerRef()).invoiceRef(a.getInvoiceRef())
                .activityDate(a.getActivityDate()).activityType(a.getActivityType())
                .notes(a.getNotes()).promisedPaymentDate(a.getPromisedPaymentDate())
                .promisedAmount(a.getPromisedAmount()).followUpDate(a.getFollowUpDate())
                .status(a.getStatus()).createdBy(a.getCreatedBy()).createdAt(a.getCreatedAt())
                .build();
    }
}
