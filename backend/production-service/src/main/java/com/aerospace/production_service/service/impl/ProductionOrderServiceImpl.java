package com.aerospace.production_service.service.impl;

import com.aerospace.production_service.client.ProductionResourceClient;
import com.aerospace.production_service.client.dto.EmployeeInfo;
import com.aerospace.production_service.client.dto.MachineInfo;
import com.aerospace.production_service.client.dto.ReservationConflictInfo;
import com.aerospace.production_service.client.dto.ToolingInfo;
import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.*;
import com.aerospace.production_service.entity.ProductionMaterial.MaterialStatus;
import com.aerospace.production_service.entity.ProductionStageLog.StageStatus;
import com.aerospace.production_service.event.ProductionOrderCreatedEvent;
import com.aerospace.production_service.event.ProductionOrderCompletedEvent;
import com.aerospace.production_service.event.WorkOrderStatusChangedEvent;
import com.aerospace.production_service.event.producer.ProductionProducer;
import com.aerospace.production_service.exception.ResourceNotFoundException;
import com.aerospace.production_service.mapper.ProductionOrderMapper;
import com.aerospace.production_service.mapper.ShopFloorNcrMapper;
import com.aerospace.production_service.repository.*;
import com.aerospace.production_service.service.ProductionOrderService;
import com.aerospace.common.numbering.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderServiceImpl implements ProductionOrderService {

    private final ProductionOrderRepository orderRepository;
    private final ProductionMaterialRepository materialRepository;
    private final ProductionStageLogRepository stageLogRepository;
    private final RoutingTemplateRepository routingTemplateRepository;
    private final RoutingOperationRepository routingOperationRepository;
    private final ShopFloorNcrRepository shopFloorNcrRepository;
    private final ProductionOrderMapper mapper;
    private final ShopFloorNcrMapper ncrMapper;
    private final ProductionProducer producer;
    private final DocumentSequenceService documentSequenceService;
    private final ProductionResourceClient productionResourceClient;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO createOrder(ProductionOrderRequestDTO dto) {
        ProductionOrder order = mapper.toEntity(dto);
        order.setPoNumber(documentSequenceService.generate("PROD_ORDER"));

        // Persist order first so we have an ID
        ProductionOrder saved = orderRepository.save(order);

        // Attach bill of materials
        if (dto.getMaterials() != null && !dto.getMaterials().isEmpty()) {
            List<ProductionMaterial> materials = dto.getMaterials().stream()
                    .map(m -> ProductionMaterial.builder()
                            .productionOrder(saved)
                            .partId(m.getPartId())
                            .partName(m.getPartName())
                            .partNumber(m.getPartNumber())
                            .batchNumber(m.getBatchNumber())
                            .quantityRequired(m.getQuantityRequired())
                            .quantityConsumed(0)
                            .quantityWasted(0)
                            .status(MaterialStatus.PENDING)
                            .remarks(m.getRemarks())
                            .build())
                    .toList();
            materialRepository.saveAll(materials);
            // saved.materials is EAGER-fetched and was already initialized empty at the
            // first orderRepository.save() above — findById() below returns the SAME
            // managed instance from the persistence context (JPA identity map) rather
            // than re-querying, so without this the response would report an empty
            // materials list even though the rows are correctly persisted.
            saved.setMaterials(materials);
        }

        // Reload with associations
        ProductionOrder reloaded = orderRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found after save"));

        // Publish Kafka event
        try {
            producer.sendProductionOrderCreated(
                ProductionOrderCreatedEvent.builder()
                    .productionOrderId(reloaded.getId())
                    .poNumber(reloaded.getPoNumber())
                    .productName(reloaded.getProductName())
                    .productCode(reloaded.getProductCode())
                    .aircraftType(reloaded.getAircraftType())
                    .plannedQuantity(reloaded.getPlannedQuantity())
                    .priority(reloaded.getPriority().name())
                    .productionLine(reloaded.getProductionLine())
                    .assignedTeam(reloaded.getAssignedTeam())
                    .createdBy(reloaded.getCreatedBy())
                    .batchNumber(reloaded.getBatchNumber())
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_CREATED publish failed for poNumber={}: {}",
                    saved.getPoNumber(), ex.getMessage());
        }

        log.info("ERP: Production order created — poNumber={}", saved.getPoNumber());
        return mapper.toDTO(reloaded);
    }

    // Auto-BOM: fires on a WorkOrder's OPEN→IN_PROGRESS transition (all material
    // lines issued). Idempotent — skips if a ProductionOrder already exists for this
    // workOrderId, so a redelivered/duplicate Kafka message can't create a second one.
    @Override
    @Transactional
    public void createFromWorkOrderIssuance(WorkOrderStatusChangedEvent event) {
        if (event.getWorkOrderId() == null) {
            return;
        }
        if (!orderRepository.findByWorkOrderId(event.getWorkOrderId()).isEmpty()) {
            log.info("[AUTO-BOM] ProductionOrder already exists for workOrderId={} — skipping",
                    event.getWorkOrderId());
            return;
        }

        ProductionOrder order = ProductionOrder.builder()
                .poNumber(documentSequenceService.generate("PROD_ORDER"))
                .woNumber(event.getWoNumber())
                .workOrderId(event.getWorkOrderId())
                .productName(event.getTaskDescription())
                .productCode(event.getTaskCode())
                .aircraftType(event.getAircraftType())
                .outputPartId(event.getOutputPartId())
                .outputPartNumber(event.getOutputPartNumber())
                .plannedQuantity(1) // WorkOrder has no finished-goods quantity field — see plan's open questions
                .status(ProductionOrderStatus.RELEASED) // material's already committed — skip the manual "Release" step
                .priority(ProductionPriority.MEDIUM)
                .build();
        ProductionOrder saved = orderRepository.save(order);

        if (event.getMaterialLineItems() != null) {
            List<ProductionMaterial> materials = event.getMaterialLineItems().stream()
                    .map(l -> ProductionMaterial.builder()
                            .productionOrder(saved)
                            .partId(l.getPartId())
                            .partName(l.getPartName())
                            .partNumber(l.getPartNumber())
                            .batchNumber(l.getBatchNumber())
                            .quantityRequired(l.getQuantityRequired())
                            .quantityConsumed(l.getQuantityIssued())
                            .quantityWasted(0)
                            .status(MaterialStatus.PENDING)
                            .build())
                    .toList();
            materialRepository.saveAll(materials);
        }

        try {
            producer.sendProductionOrderCreated(
                ProductionOrderCreatedEvent.builder()
                    .productionOrderId(saved.getId())
                    .poNumber(saved.getPoNumber())
                    .productName(saved.getProductName())
                    .productCode(saved.getProductCode())
                    .aircraftType(saved.getAircraftType())
                    .plannedQuantity(saved.getPlannedQuantity())
                    .priority(saved.getPriority().name())
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_CREATED publish failed for auto-created poNumber={}: {}",
                    saved.getPoNumber(), ex.getMessage());
        }

        log.info("[AUTO-BOM] ProductionOrder {} auto-created from WorkOrder {} ({} BOM lines)",
                saved.getPoNumber(), event.getWoNumber(),
                event.getMaterialLineItems() != null ? event.getMaterialLineItems().size() : 0);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductionOrderResponseDTO getById(Long id) {
        return mapper.toDTO(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionOrderResponseDTO getByPoNumber(String poNumber) {
        return mapper.toDTO(orderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production order not found: " + poNumber)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderResponseDTO> getAll() {
        return orderRepository.findAll().stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderResponseDTO> getByWorkOrderId(Long workOrderId) {
        return orderRepository.findByWorkOrderId(workOrderId).stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderNeedingPartResponse> findProductionOrdersNeedingPart(Long partId) {
        return materialRepository.findByPartId(partId).stream()
                .filter(m -> m.getStatus() == MaterialStatus.PENDING)
                .filter(m -> m.getProductionOrder().getWorkOrderId() == null)
                .filter(m -> m.getProductionOrder().getStatus() != ProductionOrderStatus.COMPLETED
                        && m.getProductionOrder().getStatus() != ProductionOrderStatus.CANCELLED)
                .map(m -> ProductionOrderNeedingPartResponse.builder()
                        .productionOrderId(m.getProductionOrder().getId())
                        .poNumber(m.getProductionOrder().getPoNumber())
                        .materialId(m.getId())
                        .partName(m.getPartName())
                        .quantityRequired(m.getQuantityRequired())
                        .quantityConsumed(m.getQuantityConsumed())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductionOrderResponseDTO> getAllPaged(Pageable pageable) {
        return orderRepository.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderResponseDTO> getByStatus(ProductionOrderStatus status) {
        return orderRepository.findByStatus(status).stream().map(mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionOrderResponseDTO> searchByProductName(String name) {
        return orderRepository.findByProductNameContainingIgnoreCase(name)
                .stream().map(mapper::toDTO).toList();
    }

    @Override
    public List<ProductionOrderResponseDTO> getOverdueOrders() {
        return orderRepository.findOverdueOrders(LocalDateTime.now())
                .stream().map(mapper::toDTO).toList();
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO updateOrder(Long id, ProductionOrderRequestDTO dto) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() == ProductionOrderStatus.COMPLETED ||
            order.getStatus() == ProductionOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a " + order.getStatus() + " order");
        }

        order.setProductName(dto.getProductName());
        order.setProductCode(dto.getProductCode());
        order.setOutputPartId(dto.getOutputPartId());
        order.setOutputPartNumber(dto.getOutputPartNumber());
        order.setAircraftType(dto.getAircraftType());
        order.setPlannedQuantity(dto.getPlannedQuantity());
        order.setPriority(dto.getPriority());
        order.setPlannedStartDate(dto.getPlannedStartDate());
        order.setPlannedEndDate(dto.getPlannedEndDate());
        order.setProductionLine(dto.getProductionLine());
        order.setAssignedTeam(dto.getAssignedTeam());
        order.setSupervisorName(dto.getSupervisorName());
        order.setQualityStandard(dto.getQualityStandard());
        order.setQualityNotes(dto.getQualityNotes());
        order.setBatchNumber(dto.getBatchNumber());
        order.setRemarks(dto.getRemarks());

        return mapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() == ProductionOrderStatus.IN_PRODUCTION) {
            throw new IllegalStateException("Cannot delete an order that is IN_PRODUCTION");
        }
        if (!shopFloorNcrRepository.findByProductionOrderId(id).isEmpty()) {
            throw new IllegalStateException(
                "Cannot delete: this order has Non-Conformance Records (NCRs) that must be " +
                "preserved for quality traceability. Cancel the order instead if it's no longer needed.");
        }
        orderRepository.delete(order);
        log.info("ERP: Production order deleted — id={}", id);
    }


    // ── LIFECYCLE TRANSITIONS ─────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO releaseOrder(Long id) {
        ProductionOrder order = findOrThrow(id);
        assertStatus(order, ProductionOrderStatus.PLANNED, "release");
        order.setStatus(ProductionOrderStatus.RELEASED);
        log.info("ERP: Order RELEASED — poNumber={}", order.getPoNumber());
        return mapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO startProduction(Long id, List<Long> skipRoutingForMaterialIds) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() != ProductionOrderStatus.RELEASED &&
            order.getStatus() != ProductionOrderStatus.ON_HOLD) {
            throw new IllegalStateException(
                    "Order must be RELEASED or ON_HOLD to start production");
        }

        // Routing expansion — generate the whole ordered PENDING skeleton per BOM line
        // upfront (not lazily), so the per-line stepper UI can show the full remaining
        // sequence and the final-assembly gate is a simple "last sequence COMPLETED" check.
        List<Long> skipIds = skipRoutingForMaterialIds != null ? skipRoutingForMaterialIds : List.of();
        for (ProductionMaterial material : order.getMaterials()) {
            if (skipIds.contains(material.getId())) {
                continue; // supervisor override — e.g. a bought-complete part needs no routing
            }
            RoutingTemplate template = routingTemplateRepository
                    .findFirstByPartNumberAndIsActiveTrueAndIsCurrentRevisionTrue(material.getPartNumber())
                    .orElseThrow(() -> new IllegalStateException(
                            "No active Routing Template found for part '" + material.getPartNumber()
                            + "' (BOM line id " + material.getId() + "). Create one, or pass its id in "
                            + "?skipRoutingForMaterialIds= to skip routing for this line."));

            List<RoutingOperation> ops = template.getOperations().stream()
                    .sorted(Comparator.comparing(RoutingOperation::getSequenceNumber))
                    .toList();
            for (RoutingOperation op : ops) {
                stageLogRepository.save(ProductionStageLog.builder()
                        .productionOrder(order)
                        .productionMaterial(material)
                        .sequenceNumber(op.getSequenceNumber())
                        .attemptNumber(1)
                        .routingOperationId(op.getId())
                        .operationType(op.getOperationType())
                        .stageName(op.getOperationName())
                        .stageCode(op.getOperationCode())
                        .status(StageStatus.PENDING)
                        .build());
            }
        }

        order.setStatus(ProductionOrderStatus.IN_PRODUCTION);
        order.setActualStartDate(LocalDateTime.now());
        orderRepository.save(order);

        try {
            producer.sendProductionOrderStarted(order.getId(),
                    order.getPoNumber(), order.getProductName());
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_STARTED publish failed: {}", ex.getMessage());
        }

        log.info("ERP: Order IN_PRODUCTION — poNumber={}", order.getPoNumber());
        return mapper.toDTO(orderRepository.findById(id).orElse(order));
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO moveToQualityCheck(Long id, Integer producedQty,
                                                          Integer rejectedQty) {
        ProductionOrder order = findOrThrow(id);
        // IN_ASSEMBLY: the routing-driven path (Final Assembly stage just ran).
        // IN_PRODUCTION: kept for orders created the old way, with no BOM/routing at
        // all, that never go through the assembly gate.
        if (order.getStatus() != ProductionOrderStatus.IN_PRODUCTION
                && order.getStatus() != ProductionOrderStatus.IN_ASSEMBLY) {
            throw new IllegalStateException(
                    "Cannot move to QA. Order must be IN_PRODUCTION or IN_ASSEMBLY but is " + order.getStatus());
        }

        // startAssembly() opens an order-level "Final Assembly" stage log (no
        // productionMaterial — it isn't tied to any one BOM line) but nothing ever closed
        // it out, so it sat IN_PROGRESS forever on the Stages tab even after the order
        // itself completed. Close it here, since this is the point where assembly is done.
        ProductionStageLog closedAssemblyStage = null;
        if (order.getStatus() == ProductionOrderStatus.IN_ASSEMBLY) {
            for (ProductionStageLog s : stageLogRepository.findByProductionOrderIdAndProductionMaterialIsNull(id)) {
                if (s.getStatus() == StageStatus.IN_PROGRESS) {
                    s.setStatus(StageStatus.COMPLETED);
                    s.setCompletedAt(LocalDateTime.now());
                    s.setQuantityOut(producedQty);
                    closedAssemblyStage = stageLogRepository.save(s);
                }
            }
        }

        order.setStatus(ProductionOrderStatus.QUALITY_CHECK);
        order.setProducedQuantity(producedQty != null ? producedQty : order.getProducedQuantity());
        order.setRejectedQuantity(rejectedQty != null ? rejectedQty : 0);

        // Finished-goods reject — same traceability pattern as shop-floor scrap
        // (completeInspectionStage), but not tied to any one BOM line, so
        // productionMaterial stays null. Nothing to disposition here (no single
        // material's quantityWasted applies to a whole-unit reject) — this is purely a
        // record that N finished units didn't pass.
        if (rejectedQty != null && rejectedQty > 0) {
            shopFloorNcrRepository.save(ShopFloorNcr.builder()
                    .productionOrderId(order.getId())
                    .productionMaterial(null)
                    .stageLog(closedAssemblyStage)
                    .rejectedQuantity(rejectedQty)
                    .disposition(ShopFloorDisposition.FINISHED_GOODS_REJECT)
                    .description("Finished-goods reject recorded at Send to QA (produced "
                            + (producedQty != null ? producedQty : 0) + ", rejected " + rejectedQty + ")")
                    .reportedBy(order.getSupervisorName())
                    .build());
            log.info("[NCR] Finished-goods reject recorded — order={} qty={}", order.getPoNumber(), rejectedQty);
        }

        log.info("ERP: Order moved to QUALITY_CHECK — poNumber={}", order.getPoNumber());
        return mapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO completeOrder(Long id, Boolean qualityPassed,
                                                     String qualityNotes, String closureNotes,
                                                     String outputLocationCode) {
        ProductionOrder order = findOrThrow(id);
        assertStatus(order, ProductionOrderStatus.QUALITY_CHECK, "complete");
        if(order.getOutputPartId() == null) {
            throw new IllegalStateException(
                "Cannot complete: no output part set on this order. Edit the order and select which " +
                "Part this run produces before completing."
            );
        }
        // Don't just trust the caller's flag — a "quality passed" claim must be backed
        // by every BOM line actually having a completed, passing final QA inspection
        // (readyForAssembly is only ever set true by completeInspectionStage on a real
        // passing result, so this is the same signal that gate already relies on).
        if (Boolean.TRUE.equals(qualityPassed)) {
            boolean allInspected = order.getMaterials().stream()
                    .allMatch(m -> Boolean.TRUE.equals(m.getReadyForAssembly()));
            if (!allInspected) {
                throw new IllegalStateException(
                        "Cannot mark this order as quality-passed — not every BOM line has a completed, " +
                        "passing final QA inspection yet.");
            }
        }
        List<ShopFloorNcr> openNcrs = shopFloorNcrRepository.findByProductionOrderIdAndStatus(id, NcrStatus.OPEN);
        if (!openNcrs.isEmpty()) {
            ShopFloorNcr first = openNcrs.get(0);
            throw new IllegalStateException(
                "Cannot complete: " + openNcrs.size() + " unresolved NCR(s) exist (e.g. " + first.getDisposition() +
                (first.getProductionMaterial() != null ? " on " + first.getProductionMaterial().getPartName() : "") +
                "). Resolve them first.");
        }

        order.setStatus(ProductionOrderStatus.COMPLETED);
        order.setActualEndDate(LocalDateTime.now());
        order.setQualityPassed(qualityPassed);
        order.setQualityNotes(qualityNotes);
        order.setClosureNotes(closureNotes);
        orderRepository.save(order);

        try {
            producer.sendProductionOrderCompleted(
                ProductionOrderCompletedEvent.builder()
                    .productionOrderId(order.getId())
                    .poNumber(order.getPoNumber())
                    .productName(order.getProductName())
                    .productCode(order.getProductCode())
                    .outputPartId(order.getOutputPartId())
                    .outputPartNumber(order.getOutputPartNumber())
                    .outputLocationCode(outputLocationCode)
                    .aircraftType(order.getAircraftType())
                    .plannedQuantity(order.getPlannedQuantity())
                    .producedQuantity(order.getProducedQuantity())
                    .rejectedQuantity(order.getRejectedQuantity())
                    .qualityPassed(qualityPassed)
                    .qualityNotes(qualityNotes)
                    .supervisorName(order.getSupervisorName())
                    .closureNotes(closureNotes)
                    .batchNumber(order.getBatchNumber())
                    .build()
            );
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_COMPLETED publish failed: {}", ex.getMessage());
        }

        log.info("ERP: Order COMPLETED — poNumber={}, qualityPassed={}", order.getPoNumber(), qualityPassed);
        return mapper.toDTO(order);
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO putOnHold(Long id, String reason) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() == ProductionOrderStatus.COMPLETED ||
            order.getStatus() == ProductionOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot put a " + order.getStatus() + " order on hold");
        }
        order.setStatus(ProductionOrderStatus.ON_HOLD);
        order.setRemarks(reason);
        orderRepository.save(order);

        try {
            producer.sendProductionOrderOnHold(order.getId(), order.getPoNumber(), reason);
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_ON_HOLD publish failed: {}", ex.getMessage());
        }
        return mapper.toDTO(order);
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO cancelOrder(Long id, String reason) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() == ProductionOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a COMPLETED order");
        }
        order.setStatus(ProductionOrderStatus.CANCELLED);
        order.setClosureNotes(reason);
        orderRepository.save(order);

        try {
            producer.sendProductionOrderCancelled(order.getId(), order.getPoNumber(), reason);
        } catch (Exception ex) {
            log.warn("[KAFKA] PRODUCTION_ORDER_CANCELLED publish failed: {}", ex.getMessage());
        }
        return mapper.toDTO(order);
    }

    // ── MATERIAL MANAGEMENT ───────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO consumeMaterial(Long orderId, Long materialId,
                                                      int consumed, int wasted, String remarks) {
        findOrThrow(orderId); // validate order exists
        ProductionMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material not found: " + materialId));

        if (material.getQuantityRequired() != null &&
                (consumed + wasted) > material.getQuantityRequired()) {
            throw new IllegalStateException("Consumed + Wasted exceeds required quantity");
        }

        material.setQuantityConsumed(consumed);
        material.setQuantityWasted(wasted);
        material.setStatus(MaterialStatus.CONSUMED);
        material.setRemarks(remarks);
        materialRepository.save(material);

        log.info("ERP: Material consumed — orderId={}, partId={}, qty={}",
                orderId, material.getPartId(), consumed);
        return mapper.toDTO(findOrThrow(orderId));
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO returnMaterial(Long orderId, Long materialId, String remarks) {
        findOrThrow(orderId);
        ProductionMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Material not found: " + materialId));
        material.setStatus(MaterialStatus.RETURNED);
        material.setRemarks(remarks);
        materialRepository.save(material);
        return mapper.toDTO(findOrThrow(orderId));
    }

    // ── STAGE LOGGING ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO addStageLog(Long orderId, StageLogRequestDTO dto) {
        ProductionOrder order = findOrThrow(orderId);

        ProductionStageLog stageLog = ProductionStageLog.builder()
                .productionOrder(order)
                .stageName(dto.getStageName())
                .stageCode(dto.getStageCode())
                .status(dto.getStatus())
                .quantityIn(dto.getQuantityIn())
                .quantityOut(dto.getQuantityOut())
                .scrapCount(dto.getScrapCount())
                .operatorName(dto.getOperatorName())
                .remarks(dto.getRemarks())
                .startedAt(dto.getStartedAt())
                .completedAt(dto.getCompletedAt())
                .build();

        stageLogRepository.save(stageLog);

        try {
            producer.sendStageLogAdded(orderId, order.getPoNumber(),
                    dto.getStageName(), dto.getStatus().name());
        } catch (Exception ex) {
            log.warn("[KAFKA] STAGE_LOG_ADDED publish failed: {}", ex.getMessage());
        }

        log.info("ERP: Stage log added — orderId={}, stage={}, status={}",
                orderId, dto.getStageName(), dto.getStatus());
        return mapper.toDTO(findOrThrow(orderId));
    }

    // ── ROUTING-DRIVEN PER-BOM-LINE STAGES ───────────────────────────────────

    @Override
    @Transactional
    public StageLogResponseDTO startStage(Long orderId, Long materialId, Long stageId, StartStageRequestDTO dto) {
        findOrThrow(orderId); // validate order exists
        ProductionMaterial material = findMaterialOrThrow(materialId);
        ProductionStageLog stage = findStageOrThrow(stageId);

        if (stage.getProductionMaterial() == null || !stage.getProductionMaterial().getId().equals(materialId)) {
            throw new IllegalStateException("Stage " + stageId + " does not belong to material " + materialId);
        }
        if (stage.getStatus() != StageStatus.PENDING) {
            throw new IllegalStateException("Stage must be PENDING to start (current: " + stage.getStatus() + ")");
        }
        if (dto.getMachineId() == null || dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("A machine and an employee must be selected to start this stage");
        }

        int quantityIn;
        if (stage.getSequenceNumber() != null && stage.getSequenceNumber() == 1) {
            quantityIn = material.getQuantityRequired() != null ? material.getQuantityRequired() : 0;
        } else {
            ProductionStageLog prior = stageLogRepository
                    .findFirstByProductionMaterial_IdAndSequenceNumberOrderByAttemptNumberDesc(
                            materialId, stage.getSequenceNumber() - 1)
                    .orElseThrow(() -> new IllegalStateException("Prior operation not found for sequence "
                            + (stage.getSequenceNumber() - 1)));
            if (prior.getStatus() != StageStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Prior operation (" + prior.getStageName() + ") is not complete yet");
            }
            quantityIn = prior.getQuantityOut() != null ? prior.getQuantityOut() : 0;
        }

        // Real eligibility check — not a UI dropdown filter — then lock both so nothing
        // else can start using them while this stage is running.
        MachineInfo machine = productionResourceClient.getMachine(dto.getMachineId());
        if (!"ACTIVE".equals(machine.getStatus())) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is not ACTIVE (current: " + machine.getStatus() + ")");
        }
        if (machine.isOverdue()) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is overdue for maintenance and cannot be assigned");
        }
        EmployeeInfo employee = productionResourceClient.getEmployee(dto.getEmployeeId());
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is not ACTIVE (current: " + employee.getStatus() + ")");
        }

        // Reservations are advance bookings, separate from occupancy — but a stage
        // must not start on a machine/employee someone else has reserved for the
        // exact time it's trying to start. excludeProductionOrderId=orderId means a
        // reservation that DOES belong to this order never blocks its own stage.
        ReservationConflictInfo machineConflict = productionResourceClient.getMachineReservationConflict(dto.getMachineId(), orderId);
        if (machineConflict != null) {
            throw new IllegalStateException(
                    "Machine " + machine.getMachineCode() + " is reserved by another order ("
                            + machineConflict.getReservedFrom() + " to " + machineConflict.getReservedTo()
                            + ", booked by " + machineConflict.getCreatedBy() + ")");
        }
        ReservationConflictInfo employeeConflict = productionResourceClient.getEmployeeReservationConflict(dto.getEmployeeId(), orderId);
        if (employeeConflict != null) {
            throw new IllegalStateException(
                    "Employee " + employee.getEmployeeCode() + " is reserved by another order ("
                            + employeeConflict.getReservedFrom() + " to " + employeeConflict.getReservedTo()
                            + ", booked by " + employeeConflict.getCreatedBy() + ")");
        }

        // Formal skill/tooling requirements — only enforced if the routing step
        // actually set one (both optional; a step with neither accepts any active
        // employee and needs no tool). Looked up once and reused by both checks below.
        RoutingOperation routingOp = stage.getRoutingOperationId() != null
                ? routingOperationRepository.findById(stage.getRoutingOperationId()).orElse(null)
                : null;

        if (routingOp != null && routingOp.getRequiredSkillId() != null) {
            int minLevel = routingOp.getMinimumSkillLevel() != null ? routingOp.getMinimumSkillLevel() : 1;
            boolean qualified = employee.getSkills() != null && employee.getSkills().stream().anyMatch(s ->
                    routingOp.getRequiredSkillId().equals(s.getSkillId())
                            && !"EXPIRED".equals(s.getStatus()) && !"REVOKED".equals(s.getStatus())
                            && s.getLevel() != null && s.getLevel() >= minLevel);
            if (!qualified) {
                throw new IllegalStateException(
                        "Employee " + employee.getEmployeeCode() + " does not hold '" + routingOp.getRequiredSkillName()
                                + "' at level " + minLevel + " or higher (required for this operation)");
            }
        }

        // A required tool is a specific instance (unlike the skill category match
        // above) — resolved from the routing step itself, not chosen by the operator.
        Long requiredToolingId = routingOp != null ? routingOp.getRequiredToolingId() : null;
        if (requiredToolingId != null) {
            ToolingInfo tooling = productionResourceClient.getTooling(requiredToolingId);
            if (!"ACTIVE".equals(tooling.getStatus())) {
                throw new IllegalStateException(
                        "Tool " + tooling.getToolCode() + " (required for this operation) is not ACTIVE (current: "
                                + tooling.getStatus() + ")");
            }
            if ("IN_OPERATION".equals(tooling.getOccupancy())) {
                throw new IllegalStateException(
                        "Tool " + tooling.getToolCode() + " (required for this operation) is currently in use on another operation");
            }
        }

        productionResourceClient.occupyMachine(dto.getMachineId(), stage.getId());
        try {
            productionResourceClient.occupyEmployee(dto.getEmployeeId(), stage.getId());
        } catch (RuntimeException ex) {
            // Employee lock failed after the machine lock succeeded — release the machine
            // rather than leave it falsely held for a stage that never actually started.
            productionResourceClient.releaseMachine(dto.getMachineId());
            throw ex;
        }
        if (requiredToolingId != null) {
            try {
                productionResourceClient.occupyTooling(requiredToolingId, stage.getId());
            } catch (RuntimeException ex) {
                // Tooling lock failed after machine+employee locks succeeded — release
                // both rather than leave them falsely held for a stage that never started.
                productionResourceClient.releaseEmployee(dto.getEmployeeId());
                productionResourceClient.releaseMachine(dto.getMachineId());
                throw ex;
            }
        }

        stage.setQuantityIn(quantityIn);
        stage.setStatus(StageStatus.IN_PROGRESS);
        stage.setStartedAt(LocalDateTime.now());
        stage.setMachineId(dto.getMachineId());
        stage.setEmployeeId(dto.getEmployeeId());
        stage.setToolingId(requiredToolingId);
        stage.setOperatorName(dto.getOperatorName() != null && !dto.getOperatorName().isBlank()
                ? dto.getOperatorName() : employee.getName());
        log.info("[STAGE] Started — order={} material={} stage={} (seq {}) qtyIn={} machine={} employee={}{}",
                orderId, materialId, stage.getStageName(), stage.getSequenceNumber(), quantityIn,
                machine.getMachineCode(), employee.getEmployeeCode(),
                requiredToolingId != null ? " tooling=" + requiredToolingId : "");
        return mapper.toStageLogDTO(stageLogRepository.save(stage));
    }

    // A PRODUCTION-type stage (Cutting, Drilling, ...) — reports quantity produced
    // only. No accept/reject decision here; that's the following QA_INSPECTION
    // stage's job. Always completes successfully (a production step doesn't reject).
    @Override
    @Transactional
    public StageLogResponseDTO completeProductionStage(Long orderId, Long materialId, Long stageId,
                                                        CompleteProductionStageRequestDTO dto) {
        findOrThrow(orderId);
        findMaterialOrThrow(materialId);
        ProductionStageLog stage = findStageOrThrow(stageId);

        if (stage.getOperationType() != null && stage.getOperationType() != OperationType.PRODUCTION) {
            throw new IllegalArgumentException("Stage " + stageId + " is a " + stage.getOperationType()
                    + " stage — use complete-inspection instead.");
        }
        if (stage.getStatus() != StageStatus.IN_PROGRESS) {
            throw new IllegalStateException("Stage must be IN_PROGRESS to complete (current: " + stage.getStatus() + ")");
        }

        int quantityProduced = dto.getQuantityProduced() != null ? dto.getQuantityProduced() : 0;
        stage.setQuantityOut(quantityProduced);
        stage.setStatus(StageStatus.COMPLETED);
        stage.setCompletedAt(LocalDateTime.now());
        stage.setRemarks(dto.getRemarks());
        if (dto.getOperatorName() != null && !dto.getOperatorName().isBlank()) {
            stage.setOperatorName(dto.getOperatorName());
        }

        StageLogResponseDTO result = mapper.toStageLogDTO(stageLogRepository.save(stage));
        releaseResources(stage);
        autoResolveReworkNcr(stage.getId());
        log.info("[STAGE] Production completed — order={} material={} stage={} qtyProduced={}",
                orderId, materialId, stage.getStageName(), quantityProduced);
        return result;
    }

    // A REWORK NCR names the redo stage that would clear it (reworkStageLogId) — once
    // that redo stage actually finishes successfully, the reject it was raised for has
    // genuinely been dealt with, so auto-close it rather than make someone click Resolve
    // for a real, already-completed signal. SCRAP/FINISHED_GOODS_REJECT have no such
    // signal and stay OPEN until a manual Resolve.
    private void autoResolveReworkNcr(Long completedStageId) {
        shopFloorNcrRepository.findByReworkStageLogId(completedStageId).ifPresent(ncr -> {
            if (ncr.getStatus() == NcrStatus.OPEN) {
                ncr.setStatus(NcrStatus.RESOLVED);
                ncr.setResolvedBy("SYSTEM (rework completed)");
                ncr.setResolvedAt(LocalDateTime.now());
                shopFloorNcrRepository.save(ncr);
                log.info("[NCR] Auto-resolved on rework completion — ncrId={} reworkStageLogId={}",
                        ncr.getId(), completedStageId);
            }
        });
    }

    // Frees the machine/employee this stage was holding — called on every exit path
    // (success and failure alike), so a rejected inspection still releases resources
    // for the next job. Best-effort: see ProductionResourceClient.releaseMachine/Employee.
    private void releaseResources(ProductionStageLog stage) {
        if (stage.getMachineId() != null) {
            productionResourceClient.releaseMachine(stage.getMachineId());
        }
        if (stage.getEmployeeId() != null) {
            productionResourceClient.releaseEmployee(stage.getEmployeeId());
        }
        if (stage.getToolingId() != null) {
            productionResourceClient.releaseTooling(stage.getToolingId());
        }
    }

    // The quality gate — splits quantityIn into good/rework/scrap, raises a
    // ShopFloorNcr for any non-zero rejected bucket, and (only once a BOM line's LAST
    // sequence step completes with output > 0) flips readyForAssembly and re-checks
    // the order-level final-assembly gate. Role-gated to QA_INSPECTOR/MANAGER/ADMIN.
    @Override
    @Transactional
    public StageLogResponseDTO completeInspectionStage(Long orderId, Long materialId, Long stageId, CompleteStageRequestDTO dto) {
        requireAnyRole("QA_INSPECTOR", "MANAGER", "ADMIN");

        ProductionOrder order = findOrThrow(orderId);
        ProductionMaterial material = findMaterialOrThrow(materialId);
        ProductionStageLog stage = findStageOrThrow(stageId);

        if (stage.getOperationType() != null && stage.getOperationType() != OperationType.QA_INSPECTION) {
            throw new IllegalArgumentException("Stage " + stageId + " is a " + stage.getOperationType()
                    + " stage — use complete-production instead.");
        }
        if (stage.getStatus() != StageStatus.IN_PROGRESS) {
            throw new IllegalStateException("Stage must be IN_PROGRESS to complete (current: " + stage.getStatus() + ")");
        }

        int quantityIn = stage.getQuantityIn() != null ? stage.getQuantityIn() : 0;
        int good = dto.getQuantityGood() != null ? dto.getQuantityGood() : 0;
        int rework = dto.getQuantityRework() != null ? dto.getQuantityRework() : 0;
        int scrap = dto.getQuantityScrap() != null ? dto.getQuantityScrap() : 0;
        if (good + rework + scrap != quantityIn) {
            throw new IllegalArgumentException("quantityGood + quantityRework + quantityScrap ("
                    + (good + rework + scrap) + ") must equal this stage's quantityIn (" + quantityIn + ")");
        }

        stage.setQuantityOut(good);
        stage.setScrapCount(scrap);
        stage.setStatus(good > 0 ? StageStatus.COMPLETED : StageStatus.FAILED);
        stage.setCompletedAt(LocalDateTime.now());
        stage.setRemarks(dto.getRemarks());
        if (dto.getInspectedBy() != null && !dto.getInspectedBy().isBlank()) {
            stage.setOperatorName(dto.getInspectedBy());
        }
        ProductionStageLog savedStage = stageLogRepository.save(stage);
        releaseResources(savedStage); // release on both COMPLETED and FAILED — not just success
        if (savedStage.getStatus() == StageStatus.COMPLETED) {
            autoResolveReworkNcr(savedStage.getId());
        }

        if (scrap > 0) {
            shopFloorNcrRepository.save(ShopFloorNcr.builder()
                    .productionOrderId(orderId)
                    .productionMaterial(material)
                    .stageLog(savedStage)
                    .rejectedQuantity(scrap)
                    .disposition(ShopFloorDisposition.SCRAP)
                    .description(dto.getRemarks())
                    .reportedBy(dto.getInspectedBy())
                    .build());
            int alreadyWasted = material.getQuantityWasted() != null ? material.getQuantityWasted() : 0;
            material.setQuantityWasted(alreadyWasted + scrap);
            materialRepository.save(material);
            log.info("[NCR] Shop-floor SCRAP raised — order={} material={} stage={} qty={}",
                    order.getPoNumber(), materialId, savedStage.getStageName(), scrap);
        }

        if (rework > 0) {
            // QA can't itself "redo" a production operation — send the rework qty back
            // to the PRECEDING production stage for the operator to redo, and queue a
            // new attempt of THIS inspection stage to pick up that redo's output once
            // it completes (startStage already always reads "the latest attempt of
            // sequenceNumber - 1", so no other change is needed for that hand-off).
            Integer priorSequence = savedStage.getSequenceNumber() != null ? savedStage.getSequenceNumber() - 1 : null;
            ProductionStageLog priorLatestAttempt = priorSequence != null
                    ? stageLogRepository.findFirstByProductionMaterial_IdAndSequenceNumberOrderByAttemptNumberDesc(
                            materialId, priorSequence).orElse(null)
                    : null;

            ProductionStageLog reworkAttempt;
            if (priorLatestAttempt != null) {
                reworkAttempt = stageLogRepository.save(ProductionStageLog.builder()
                        .productionOrder(order)
                        .productionMaterial(material)
                        .sequenceNumber(priorLatestAttempt.getSequenceNumber())
                        .attemptNumber(priorLatestAttempt.getAttemptNumber() + 1)
                        .routingOperationId(priorLatestAttempt.getRoutingOperationId())
                        .operationType(priorLatestAttempt.getOperationType())
                        .stageName(priorLatestAttempt.getStageName())
                        .stageCode(priorLatestAttempt.getStageCode())
                        .status(StageStatus.PENDING)
                        .quantityIn(rework)
                        .build());

                // Queue the next attempt of THIS inspection stage — PENDING until the
                // redo above completes and starts it (quantityIn filled in then).
                stageLogRepository.save(ProductionStageLog.builder()
                        .productionOrder(order)
                        .productionMaterial(material)
                        .sequenceNumber(savedStage.getSequenceNumber())
                        .attemptNumber(savedStage.getAttemptNumber() + 1)
                        .routingOperationId(savedStage.getRoutingOperationId())
                        .operationType(savedStage.getOperationType())
                        .stageName(savedStage.getStageName())
                        .stageCode(savedStage.getStageCode())
                        .status(StageStatus.PENDING)
                        .build());
            } else {
                // No preceding production stage (this QA_INSPECTION is sequence 1) —
                // fall back to retrying the inspection stage itself from scratch.
                reworkAttempt = stageLogRepository.save(ProductionStageLog.builder()
                        .productionOrder(order)
                        .productionMaterial(material)
                        .sequenceNumber(savedStage.getSequenceNumber())
                        .attemptNumber(savedStage.getAttemptNumber() + 1)
                        .routingOperationId(savedStage.getRoutingOperationId())
                        .operationType(savedStage.getOperationType())
                        .stageName(savedStage.getStageName())
                        .stageCode(savedStage.getStageCode())
                        .status(StageStatus.PENDING)
                        .quantityIn(rework)
                        .build());
            }

            shopFloorNcrRepository.save(ShopFloorNcr.builder()
                    .productionOrderId(orderId)
                    .productionMaterial(material)
                    .stageLog(savedStage)
                    .rejectedQuantity(rework)
                    .disposition(ShopFloorDisposition.REWORK)
                    .description(dto.getRemarks())
                    .reportedBy(dto.getInspectedBy())
                    .reworkStageLogId(reworkAttempt.getId())
                    .build());
            log.info("[NCR] Shop-floor REWORK raised — order={} material={} stage={} qty={} reworkAttemptId={}",
                    order.getPoNumber(), materialId, savedStage.getStageName(), rework, reworkAttempt.getId());
        }

        boolean isLastSequenceForMaterial = savedStage.getSequenceNumber() != null
                && savedStage.getSequenceNumber().equals(
                        stageLogRepository.findByProductionMaterial_IdOrderBySequenceNumberAscAttemptNumberAsc(materialId)
                                .stream().mapToInt(ProductionStageLog::getSequenceNumber).max().orElse(Integer.MIN_VALUE));

        if (isLastSequenceForMaterial && good > 0) {
            material.setReadyForAssembly(true);
            materialRepository.save(material);
            checkFinalAssemblyGate(orderId);
        }

        return mapper.toStageLogDTO(savedStage);
    }

    // Throws AccessDeniedException (→ 403 via Spring Security's default handling,
    // since it propagates out of MVC's exception resolution to ExceptionTranslationFilter)
    // unless the caller's JWT authorities include one of the given roles.
    private void requireAnyRole(String... allowedRoles) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        java.util.Set<String> allowed = java.util.Set.of(allowedRoles);
        boolean hasRole = auth != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .anyMatch(allowed::contains);
        if (!hasRole) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Requires one of roles: " + String.join(", ", allowedRoles));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageLogResponseDTO> getStagesForMaterial(Long orderId, Long materialId) {
        return stageLogRepository.findByProductionMaterial_IdOrderBySequenceNumberAscAttemptNumberAsc(materialId)
                .stream().map(mapper::toStageLogDTO).toList();
    }

    // ── FINAL ASSEMBLY ────────────────────────────────────────────────────────

    // Runs after every stage completion — flips the order to READY_FOR_ASSEMBLY the
    // moment every BOM line has finished its own routing (in parallel), never
    // auto-advancing past that: starting/completing the assembly itself is a human action.
    private void checkFinalAssemblyGate(Long orderId) {
        ProductionOrder order = findOrThrow(orderId);
        if (order.getStatus() != ProductionOrderStatus.IN_PRODUCTION || order.getMaterials().isEmpty()) {
            return;
        }
        boolean allReady = order.getMaterials().stream()
                .allMatch(m -> Boolean.TRUE.equals(m.getReadyForAssembly()));
        if (allReady) {
            order.setStatus(ProductionOrderStatus.READY_FOR_ASSEMBLY);
            orderRepository.save(order);
            log.info("[ASSEMBLY-GATE] Order {} is READY_FOR_ASSEMBLY — all {} BOM lines ready",
                    order.getPoNumber(), order.getMaterials().size());
        }
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO startAssembly(Long orderId) {
        ProductionOrder order = findOrThrow(orderId);
        assertStatus(order, ProductionOrderStatus.READY_FOR_ASSEMBLY, "start final assembly");
        order.setStatus(ProductionOrderStatus.IN_ASSEMBLY);
        orderRepository.save(order);

        stageLogRepository.save(ProductionStageLog.builder()
                .productionOrder(order)
                .productionMaterial(null) // order-level stage — the only kind that's ever null
                .stageName("Final Assembly")
                .status(StageStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build());

        log.info("ERP: Order IN_ASSEMBLY — poNumber={}", order.getPoNumber());
        return mapper.toDTO(orderRepository.findById(orderId).orElse(order));
    }

    // ── SHOP-FLOOR NCRs ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ShopFloorNcrResponseDTO> getNcrsForOrder(Long orderId) {
        return shopFloorNcrRepository.findByProductionOrderId(orderId).stream().map(ncrMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopFloorNcrResponseDTO> getNcrsForMaterial(Long materialId) {
        return shopFloorNcrRepository.findByProductionMaterial_Id(materialId).stream().map(ncrMapper::toDTO).toList();
    }

    @Override
    @Transactional
    public ShopFloorNcrResponseDTO resolveNcr(Long ncrId, String resolvedBy, String resolutionRemarks) {
        ShopFloorNcr ncr = shopFloorNcrRepository.findById(ncrId)
                .orElseThrow(() -> new ResourceNotFoundException("NCR not found: " + ncrId));
        if (ncr.getStatus() == NcrStatus.RESOLVED) {
            throw new IllegalStateException("NCR " + ncrId + " is already resolved");
        }
        ncr.setStatus(NcrStatus.RESOLVED);
        ncr.setResolvedBy(resolvedBy);
        ncr.setResolvedAt(LocalDateTime.now());
        ncr.setResolutionRemarks(resolutionRemarks);
        ShopFloorNcr saved = shopFloorNcrRepository.save(ncr);
        log.info("[NCR] Resolved manually — ncrId={} resolvedBy={}", ncrId, resolvedBy);
        return ncrMapper.toDTO(saved);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private ProductionOrder findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production order not found with id: " + id));
    }

    private ProductionMaterial findMaterialOrThrow(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));
    }

    private ProductionStageLog findStageOrThrow(Long stageId) {
        return stageLogRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Stage not found: " + stageId));
    }

    private void assertStatus(ProductionOrder order, ProductionOrderStatus required, String action) {
        if (order.getStatus() != required) {
            throw new IllegalStateException(
                    "Cannot " + action + ". Order must be in " + required
                    + " status but is " + order.getStatus());
        }
    }

}
