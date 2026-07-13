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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    .taskDescription(wo.getTaskDescription())
                    .priority(wo.getPriority().name())
                    .fromStatus(from.name())
                    .toStatus(to.name())
                    .changedBy(req.getChangedBy())
                    .remarks(req.getRemarks())
                    .changedAt(LocalDateTime.now())
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

        // ── Accumulate quantity (don't overwrite) ──────────────────────────────
        int alreadyIssued = line.getQuantityIssued() != null ? line.getQuantityIssued() : 0;
        int newTotalIssued = alreadyIssued + req.getQuantityIssued();

        line.setQuantityIssued(newTotalIssued);
        line.setBatchNumber(req.getBatchNumber());

        // ── Set status based on whether fully or partially fulfilled ───────────
        if (newTotalIssued >= line.getQuantityRequired()) {
            line.setStatus(MaterialLineItem.LineItemStatus.ISSUED);
        } else {
            line.setStatus(MaterialLineItem.LineItemStatus.PARTIALLY_ISSUED);
        }

        lineItemRepository.save(line);

        // ── Publish material request event ─────────────────────────────────────
        try {
            producer.publishMaterialRequest(MaterialRequestEvent.builder()
                    .workOrderId(wo.getId())
                    .woNumber(wo.getWoNumber())
                    .partId(line.getPartId())
                    .partName(line.getPartName())
                    .batchNumber(req.getBatchNumber())
                    .quantityRequested(req.getQuantityIssued())
                    .requestedBy(req.getIssuedBy())
                    .location(wo.getLocation())
                    .requestedAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed to publish MATERIAL_REQUEST: {}", ex.getMessage());
        }

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

        log.info("[WO] Material issued → WO={} partId={} qtyThisTime={} totalIssued={} batch={}",
                wo.getWoNumber(), line.getPartId(), req.getQuantityIssued(), newTotalIssued, req.getBatchNumber());
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
                        .build())
                .toList();

        int totalRequired = lines.stream()
                .mapToInt(l -> l.getQuantityRequired() != null ? l.getQuantityRequired() : 0).sum();
        int totalIssued = lines.stream()
                .mapToInt(l -> l.getQuantityIssued() != null ? l.getQuantityIssued() : 0).sum();
        boolean allReady = !lines.isEmpty() && lines.stream()
                .allMatch(l -> l.getStatus() == MaterialLineItem.LineItemStatus.ISSUED
                            || l.getStatus() == MaterialLineItem.LineItemStatus.CANCELLED);

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
                .materialLineItems(lines)
                .totalPartsRequired(totalRequired)
                .totalPartsIssued(totalIssued)
                .allMaterialReady(allReady)
                .build();
    }
}