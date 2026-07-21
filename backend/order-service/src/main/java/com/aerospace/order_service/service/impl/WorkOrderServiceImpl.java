package com.aerospace.order_service.service.impl;

import com.aerospace.order_service.dto.WorkOrderDTOs.*;
import com.aerospace.order_service.entity.*;
import com.aerospace.order_service.event.WorkOrderEvents.*;
import com.aerospace.order_service.event.producer.WorkOrderProducer;
import com.aerospace.order_service.exception.ResourceNotFoundException;
import com.aerospace.order_service.exception.InvalidStatusTransitionException;
import com.aerospace.order_service.repository.*;
import com.aerospace.order_service.service.WorkOrderService;
import com.aerospace.order_service.service.WoNumberGeneratorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final MaterialLineItemRepository lineItemRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final WorkOrderProducer producer;
    private final WoNumberGeneratorService woNumberGenerator;
    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.inventory.base-url}")
    private String inventoryBaseUrl;

    // Valid status transitions — prevents illegal state changes
    private static final java.util.Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS =
        java.util.Map.of(
            WorkOrderStatus.DRAFT,       Set.of(WorkOrderStatus.OPEN, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.OPEN,        Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.IN_PROGRESS, Set.of(WorkOrderStatus.ON_HOLD, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.ON_HOLD,     Set.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED),
            WorkOrderStatus.COMPLETED,   Set.of(),
            WorkOrderStatus.CANCELLED,   Set.of()
        );

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public WorkOrderResponse createWorkOrder(CreateWorkOrderRequest req) {
        String woNumber = woNumberGenerator.generate();

        WorkOrder wo = WorkOrder.builder()
                .woNumber(woNumber)
                .aircraftId(req.getAircraftId())
                .aircraftType(req.getAircraftType())
                .taskDescription(req.getTaskDescription())
                .taskCode(req.getTaskCode())
                .priority(req.getPriority())
                .assignedTo(req.getAssignedTo())
                .createdBy(req.getCreatedBy())
                .location(req.getLocation())
                .dueDate(req.getDueDate())
                .remarks(req.getRemarks())
                .outputPartId(req.getOutputPartId())
                .outputPartNumber(req.getOutputPartNumber())
                .status(WorkOrderStatus.DRAFT)
                .build();

        WorkOrder saved = workOrderRepository.save(wo);

        // Add material line items if provided
        if (req.getMaterialItems() != null) {
            req.getMaterialItems().forEach(item -> {
                MaterialLineItem line = MaterialLineItem.builder()
                        .workOrder(saved)
                        .partId(item.getPartId())
                        .partName(item.getPartName())
                        .partNumber(item.getPartNumber())
                        .quantityRequired(item.getQuantityRequired())
                        .quantityIssued(0)
                        .status(MaterialLineItem.LineItemStatus.PENDING)
                        .remarks(item.getRemarks())
                        .unitPrice(item.getUnitPrice())
                        .taxRatePercent(item.getTaxRatePercent())
                        .description(item.getDescription())
                        .build();
                lineItemRepository.save(line);
            });
        }

        // Log initial status
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(saved.getId())
                .woNumber(woNumber)
                .toStatus(WorkOrderStatus.DRAFT)
                .changedBy(req.getCreatedBy())
                .remarks("Work order created")
                .build());

        // Publish Kafka event
        try {
            List<MaterialLineSummary> materialLines = req.getMaterialItems() == null
                    ? List.of()
                    : req.getMaterialItems().stream()
                            .filter(item -> item.getPartId() != null)
                            .map(item -> MaterialLineSummary.builder()
                                    .partId(item.getPartId())
                                    .quantityRequired(item.getQuantityRequired())
                                    .build())
                            .toList();

            producer.publishCreated(WorkOrderCreatedEvent.builder()
                    .workOrderId(saved.getId())
                    .woNumber(woNumber)
                    .aircraftId(saved.getAircraftId())
                    .taskDescription(saved.getTaskDescription())
                    .priority(saved.getPriority().name())
                    .status(saved.getStatus().name())
                    .createdBy(saved.getCreatedBy())
                    .location(saved.getLocation())
                    .createdAt(saved.getCreatedAt())
                    .materialLines(materialLines)
                    .build());
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed to publish WORK_ORDER_CREATED for {}: {}", woNumber, ex.getMessage());
        }

        log.info("[WO] Created {} for aircraft={} priority={}",
                woNumber, saved.getAircraftId(), saved.getPriority());
        return toResponse(workOrderRepository.findById(saved.getId()).orElse(saved));
    }

    // ── STATUS TRANSITION ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public WorkOrderResponse transitionStatus(Long id, StatusTransitionRequest req) {
        WorkOrder wo = findById(id);
        WorkOrderStatus from = wo.getStatus();
        WorkOrderStatus to = req.getNewStatus();

        // Validate transition
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + from + " to " + to);
        }

        wo.setStatus(to);
        if (to == WorkOrderStatus.IN_PROGRESS && wo.getStartedAt() == null) {
            wo.setStartedAt(LocalDateTime.now());
        }
        if (to == WorkOrderStatus.COMPLETED) {
            wo.setCompletedAt(LocalDateTime.now());
            wo.setClosureNotes(req.getRemarks());
        }
        workOrderRepository.save(wo);

        // Save history
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrderId(wo.getId())
                .woNumber(wo.getWoNumber())
                .fromStatus(from)
                .toStatus(to)
                .changedBy(req.getChangedBy())
                .remarks(req.getRemarks())
                .build());

        // Publish Kafka event
        try {
            var event = WorkOrderStatusChangedEvent.builder()
                    .workOrderId(wo.getId())
                    .woNumber(wo.getWoNumber())
                    .aircraftId(wo.getAircraftId())
                    .aircraftType(wo.getAircraftType())
                    .taskDescription(wo.getTaskDescription())
                    .taskCode(wo.getTaskCode())
                    .priority(wo.getPriority().name())
                    .fromStatus(from.name())
                    .toStatus(to.name())
                    .changedBy(req.getChangedBy())
                    .remarks(req.getRemarks())
                    .changedAt(LocalDateTime.now())
                    .outputPartId(wo.getOutputPartId())
                    .outputPartNumber(wo.getOutputPartNumber())
                    // Auto-BOM trigger for production-service — only carry the material
                    // snapshot on the transition where every line has just been issued.
                    .materialLineItems(to == WorkOrderStatus.IN_PROGRESS
                            ? wo.getMaterialLineItems().stream()
                                    .map(l -> MaterialLineSnapshot.builder()
                                            .partId(l.getPartId())
                                            .partName(l.getPartName())
                                            .partNumber(l.getPartNumber())
                                            .batchNumber(l.getBatchNumber())
                                            .quantityRequired(l.getQuantityRequired())
                                            .quantityIssued(l.getQuantityIssued())
                                            .build())
                                    .toList()
                            : null)
                    .build();

            if (to == WorkOrderStatus.COMPLETED) {
                producer.publishCompleted(WorkOrderCompletedEvent.builder()
                        .workOrderId(wo.getId())
                        .woNumber(wo.getWoNumber())
                        .aircraftId(wo.getAircraftId())
                        .taskDescription(wo.getTaskDescription())
                        .completedBy(req.getChangedBy())
                        .closureNotes(req.getRemarks())
                        .completedAt(wo.getCompletedAt())
                        .build());
            } else {
                producer.publishStatusChanged(event);
            }
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed to publish status change for {}: {}", wo.getWoNumber(), ex.getMessage());
        }

        log.info("[WO] {} transitioned {} → {}", wo.getWoNumber(), from, to);
        return toResponse(workOrderRepository.findById(id).orElse(wo));
    }

    // ── ISSUE MATERIAL ────────────────────────────────────────────────────────
    @Override
    @Transactional
    public WorkOrderResponse issueMaterial(Long workOrderId, IssueMaterialRequest req) {
        WorkOrder wo = findById(workOrderId);

        MaterialLineItem line = lineItemRepository.findById(req.getLineItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Line item not found: " + req.getLineItemId()));

        // ── Real stock check + deduction happens in inventory-service, synchronously —
        // this is what actually validates there's enough APPROVED stock before we let the
        // line item (and possibly the whole WO) move to ISSUED/IN_PROGRESS. A Kafka event
        // here would be fire-and-forget with no way to reject an over-issue in time.
        String url = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/{partId}/issue-to-production")
                .queryParam("qty", req.getQuantityIssued())
                .queryParamIfPresent("batchNumber", java.util.Optional.ofNullable(req.getBatchNumber()))
                .queryParam("workOrderId", wo.getId())
                .queryParam("woNumber", wo.getWoNumber())
                .queryParam("lineItemId", line.getId())
                .buildAndExpand(line.getPartId())
                .toUriString();
        try {
            // Forward the caller's own JWT — inventory-service's security filter requires
            // one, and without it every call here would be rejected at 403 before ever
            // reaching the real stock check (that's exactly what an unauthenticated call
            // did when this was first tested).
            HttpHeaders headers = new HttpHeaders();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }
            restTemplate.postForEntity(url, new HttpEntity<>(headers), Object.class);
        } catch (HttpStatusCodeException ex) {
            throw new InvalidStatusTransitionException(
                    "Inventory rejected this issue: " + ex.getResponseBodyAsString());
        }

        // Inventory confirmed the deduction — now just record the bookkeeping.
        return recordMaterialIssued(workOrderId, req.getLineItemId(), req.getQuantityIssued(), req.getBatchNumber());
    }

    // ── RECORD MATERIAL ISSUED (bookkeeping only) ───────────────────────────────
    // No call to inventory-service here — this is invoked either right after
    // issueMaterial's own successful deduction above, or by WorkOrderConsumer.onMaterialIssued
    // once inventory-service has ALREADY deducted stock and published the event. Calling
    // inventory again in that second case would double-deduct.
    @Override
    @Transactional
    public WorkOrderResponse recordMaterialIssued(Long workOrderId, Long lineItemId, int quantityIssued, String batchNumber) {
        WorkOrder wo = findById(workOrderId);
        MaterialLineItem line = lineItemRepository.findById(lineItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Line item not found: " + lineItemId));

        // ── Accumulate quantity (don't overwrite) ───────────────────────────────
        int alreadyIssued = line.getQuantityIssued() != null ? line.getQuantityIssued() : 0;
        int newTotalIssued = alreadyIssued + quantityIssued;

        line.setQuantityIssued(newTotalIssued);
        line.setBatchNumber(batchNumber);

        // ── Set status based on whether fully or partially fulfilled ───────────
        if (newTotalIssued >= line.getQuantityRequired()) {
            line.setStatus(MaterialLineItem.LineItemStatus.ISSUED);
        } else {
            line.setStatus(MaterialLineItem.LineItemStatus.PARTIALLY_ISSUED);
        }

        lineItemRepository.save(line);

        // ── Auto-transition to IN_PROGRESS only when ALL lines are fully issued ─
        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            boolean allIssued = wo.getMaterialLineItems().stream()
                    .allMatch(l -> l.getStatus() == MaterialLineItem.LineItemStatus.ISSUED
                                || l.getStatus() == MaterialLineItem.LineItemStatus.CANCELLED);
            if (allIssued) {
                transitionStatus(workOrderId, StatusTransitionRequest.builder()
                        .newStatus(WorkOrderStatus.IN_PROGRESS)
                        .changedBy("System")
                        .remarks("All material issued — auto-started")
                        .build());
            }
        }

        log.info("[WO] Material recorded → WO={} partId={} qtyThisTime={} totalIssued={} batch={}",
                wo.getWoNumber(), line.getPartId(), quantityIssued, newTotalIssued, batchNumber);
        return toResponse(workOrderRepository.findById(workOrderId).orElse(wo));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    public WorkOrderResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public WorkOrderResponse getByWoNumber(String woNumber) {
        return toResponse(workOrderRepository.findByWoNumber(woNumber)
                .orElseThrow(() -> new ResourceNotFoundException("WO not found: " + woNumber)));
    }

    @Override
    public List<WorkOrderResponse> getAll() {
        return workOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> getByStatus(String status) {
        return workOrderRepository.findByStatusOrderByCreatedAtDesc(
                WorkOrderStatus.valueOf(status.toUpperCase()))
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> getByPriority(String priority) {
        return workOrderRepository.findByPriority(Priority.valueOf(priority.toUpperCase()))
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> getByAircraft(String aircraftId) {
        return workOrderRepository.findByAircraftId(aircraftId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> getOverdue() {
        return workOrderRepository.findOverdue(LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> search(String query) {
        return workOrderRepository.search(query)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderResponse> getRecent() {
        return workOrderRepository.findTop10ByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkOrderNeedingPartResponse> findWorkOrdersNeedingPart(Long partId) {
        return lineItemRepository.findByPartId(partId).stream()
                .filter(l -> l.getStatus() == MaterialLineItem.LineItemStatus.PENDING
                        || l.getStatus() == MaterialLineItem.LineItemStatus.PARTIALLY_ISSUED)
                .filter(l -> l.getWorkOrder().getStatus() != WorkOrderStatus.COMPLETED
                        && l.getWorkOrder().getStatus() != WorkOrderStatus.CANCELLED)
                .map(l -> WorkOrderNeedingPartResponse.builder()
                        .workOrderId(l.getWorkOrder().getId())
                        .woNumber(l.getWorkOrder().getWoNumber())
                        .lineItemId(l.getId())
                        .taskDescription(l.getWorkOrder().getTaskDescription())
                        .quantityRequired(l.getQuantityRequired())
                        .quantityIssued(l.getQuantityIssued())
                        .build())
                .toList();
    }

    @Override
    public List<StatusHistoryResponse> getStatusHistory(Long workOrderId) {
        return historyRepository.findByWorkOrderIdOrderByChangedAtDesc(workOrderId)
                .stream().map(h -> StatusHistoryResponse.builder()
                        .id(h.getId())
                        .woNumber(h.getWoNumber())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .changedBy(h.getChangedBy())
                        .remarks(h.getRemarks())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();
    }

    // ── UPDATE / DELETE ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public WorkOrderResponse updateWorkOrder(Long id, UpdateWorkOrderRequest req) {
        WorkOrder wo = findById(id);
        if (req.getTaskDescription() != null) wo.setTaskDescription(req.getTaskDescription());
        if (req.getTaskCode()        != null) wo.setTaskCode(req.getTaskCode());
        if (req.getPriority()        != null) wo.setPriority(req.getPriority());
        if (req.getAssignedTo()      != null) wo.setAssignedTo(req.getAssignedTo());
        if (req.getLocation()        != null) wo.setLocation(req.getLocation());
        if (req.getDueDate()         != null) wo.setDueDate(req.getDueDate());
        if (req.getRemarks()         != null) wo.setRemarks(req.getRemarks());
        if (req.getOutputPartId()    != null) wo.setOutputPartId(req.getOutputPartId());
        if (req.getOutputPartNumber() != null) wo.setOutputPartNumber(req.getOutputPartNumber());
        return toResponse(workOrderRepository.save(wo));
    }

    @Override
    @Transactional
    public WorkOrderResponse addMaterialLineItem(Long workOrderId, MaterialLineItemRequest req) {
        WorkOrder wo = findById(workOrderId);
        MaterialLineItem line = MaterialLineItem.builder()
                .workOrder(wo)
                .partId(req.getPartId())
                .partName(req.getPartName())
                .partNumber(req.getPartNumber())
                .quantityRequired(req.getQuantityRequired())
                .quantityIssued(0)
                .status(MaterialLineItem.LineItemStatus.PENDING)
                .remarks(req.getRemarks())
                .unitPrice(req.getUnitPrice())
                .taxRatePercent(req.getTaxRatePercent())
                .description(req.getDescription())
                .build();
        lineItemRepository.save(line);
        return toResponse(workOrderRepository.findById(workOrderId).orElse(wo));
    }

    @Override
    @Transactional
    public void deleteWorkOrder(Long id) {
        WorkOrder wo = findById(id);
        // Anything past DRAFT may already have material issued against it or be tracked in
        // downstream reports — deleting it would silently orphan that history, so only a
        // never-opened draft is safe to remove outright.
        if (wo.getStatus() != WorkOrderStatus.DRAFT) {
            throw new InvalidStatusTransitionException(
                    "Only a DRAFT work order can be deleted (current: " + wo.getStatus() + "). Cancel it instead.");
        }
        workOrderRepository.deleteById(id);
    }

    // ── DASHBOARD STATS ───────────────────────────────────────────────────────

    @Override
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalWorkOrders(workOrderRepository.count())
                .draft(workOrderRepository.countByStatus(WorkOrderStatus.DRAFT))
                .open(workOrderRepository.countByStatus(WorkOrderStatus.OPEN))
                .inProgress(workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS))
                .onHold(workOrderRepository.countByStatus(WorkOrderStatus.ON_HOLD))
                .completed(workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED))
                .cancelled(workOrderRepository.countByStatus(WorkOrderStatus.CANCELLED))
                .criticalPriority(workOrderRepository.countByPriority(Priority.CRITICAL))
                .overdueCount(workOrderRepository.findOverdue(LocalDateTime.now()).size())
                .build();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private WorkOrder findById(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
    }

    private WorkOrderResponse toResponse(WorkOrder wo) {
        List<MaterialLineItemResponse> lines = wo.getMaterialLineItems().stream()
                .map(l -> MaterialLineItemResponse.builder()
                        .id(l.getId())
                        .partId(l.getPartId())
                        .partName(l.getPartName())
                        .partNumber(l.getPartNumber())
                        .batchNumber(l.getBatchNumber())
                        .quantityRequired(l.getQuantityRequired())
                        .quantityIssued(l.getQuantityIssued())
                        .status(l.getStatus())
                        .remarks(l.getRemarks())
                        .unitPrice(l.getUnitPrice())
                        .taxRatePercent(l.getTaxRatePercent())
                        .description(l.getDescription())
                        .build())
                .toList();

        int totalRequired = lines.stream()
                .mapToInt(l -> l.getQuantityRequired() != null ? l.getQuantityRequired() : 0).sum();
        int totalIssued = lines.stream()
                .mapToInt(l -> l.getQuantityIssued() != null ? l.getQuantityIssued() : 0).sum();
        boolean allReady = !lines.isEmpty() && lines.stream()
                .allMatch(l -> l.getStatus() == MaterialLineItem.LineItemStatus.ISSUED
                            || l.getStatus() == MaterialLineItem.LineItemStatus.CANCELLED);

        java.math.BigDecimal totalMaterialCost = lines.stream()
                .filter(l -> l.getUnitPrice() != null && l.getQuantityRequired() != null)
                .map(l -> {
                    java.math.BigDecimal base = l.getUnitPrice()
                            .multiply(java.math.BigDecimal.valueOf(l.getQuantityRequired()));
                    java.math.BigDecimal taxMultiplier = java.math.BigDecimal.ONE.add(
                            (l.getTaxRatePercent() != null ? l.getTaxRatePercent() : java.math.BigDecimal.ZERO)
                                    .divide(java.math.BigDecimal.valueOf(100)));
                    return base.multiply(taxMultiplier);
                })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return WorkOrderResponse.builder()
                .id(wo.getId())
                .woNumber(wo.getWoNumber())
                .aircraftId(wo.getAircraftId())
                .aircraftType(wo.getAircraftType())
                .taskDescription(wo.getTaskDescription())
                .taskCode(wo.getTaskCode())
                .status(wo.getStatus())
                .priority(wo.getPriority())
                .assignedTo(wo.getAssignedTo())
                .createdBy(wo.getCreatedBy())
                .location(wo.getLocation())
                .dueDate(wo.getDueDate())
                .startedAt(wo.getStartedAt())
                .completedAt(wo.getCompletedAt())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .remarks(wo.getRemarks())
                .closureNotes(wo.getClosureNotes())
                .outputPartId(wo.getOutputPartId())
                .outputPartNumber(wo.getOutputPartNumber())
                .materialLineItems(lines)
                .totalPartsRequired(totalRequired)
                .totalPartsIssued(totalIssued)
                .allMaterialReady(allReady)
                .totalMaterialCost(totalMaterialCost)
                .build();
    }
}