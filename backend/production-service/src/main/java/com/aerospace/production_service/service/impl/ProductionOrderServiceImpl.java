package com.aerospace.production_service.service.impl;

import com.aerospace.production_service.dto.*;
import com.aerospace.production_service.entity.*;
import com.aerospace.production_service.entity.ProductionMaterial.MaterialStatus;
import com.aerospace.production_service.event.ProductionOrderCreatedEvent;
import com.aerospace.production_service.event.ProductionOrderCompletedEvent;
import com.aerospace.production_service.event.producer.ProductionProducer;
import com.aerospace.production_service.exception.ResourceNotFoundException;
import com.aerospace.production_service.mapper.ProductionOrderMapper;
import com.aerospace.production_service.repository.*;
import com.aerospace.production_service.service.ProductionOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderServiceImpl implements ProductionOrderService {

    private final ProductionOrderRepository orderRepository;
    private final ProductionMaterialRepository materialRepository;
    private final ProductionStageLogRepository stageLogRepository;
    private final ProductionOrderMapper mapper;
    private final ProductionProducer producer;

    // Simple in-memory sequence for PO number generation (replace with DB sequence if needed)
    private static final AtomicLong PO_COUNTER = new AtomicLong(1);

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductionOrderResponseDTO createOrder(ProductionOrderRequestDTO dto) {
        ProductionOrder order = mapper.toEntity(dto);
        order.setPoNumber(generatePoNumber());

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
    public ProductionOrderResponseDTO startProduction(Long id) {
        ProductionOrder order = findOrThrow(id);
        if (order.getStatus() != ProductionOrderStatus.RELEASED &&
            order.getStatus() != ProductionOrderStatus.ON_HOLD) {
            throw new IllegalStateException(
                    "Order must be RELEASED or ON_HOLD to start production");
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
        return mapper.toDTO(order);
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO moveToQualityCheck(Long id, Integer producedQty,
                                                          Integer rejectedQty) {
        ProductionOrder order = findOrThrow(id);
        assertStatus(order, ProductionOrderStatus.IN_PRODUCTION, "move to QA");
        order.setStatus(ProductionOrderStatus.QUALITY_CHECK);
        order.setProducedQuantity(producedQty != null ? producedQty : order.getProducedQuantity());
        order.setRejectedQuantity(rejectedQty != null ? rejectedQty : 0);
        log.info("ERP: Order moved to QUALITY_CHECK — poNumber={}", order.getPoNumber());
        return mapper.toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public ProductionOrderResponseDTO completeOrder(Long id, Boolean qualityPassed,
                                                     String qualityNotes, String closureNotes) {
        ProductionOrder order = findOrThrow(id);
        assertStatus(order, ProductionOrderStatus.QUALITY_CHECK, "complete");
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

    // ── Private Helpers ───────────────────────────────────────────────────────

    private ProductionOrder findOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Production order not found with id: " + id));
    }

    private void assertStatus(ProductionOrder order, ProductionOrderStatus required, String action) {
        if (order.getStatus() != required) {
            throw new IllegalStateException(
                    "Cannot " + action + ". Order must be in " + required
                    + " status but is " + order.getStatus());
        }
    }

    private String generatePoNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String seq = String.format("%05d", PO_COUNTER.getAndIncrement());
        return "PO-" + year + "-" + seq;
    }
}
