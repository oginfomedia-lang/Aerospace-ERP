export enum ProductionOrderStatus {
  PLANNED = 'PLANNED',
  RELEASED = 'RELEASED',
  IN_PRODUCTION = 'IN_PRODUCTION',
  READY_FOR_ASSEMBLY = 'READY_FOR_ASSEMBLY',
  IN_ASSEMBLY = 'IN_ASSEMBLY',
  QUALITY_CHECK = 'QUALITY_CHECK',
  COMPLETED = 'COMPLETED',
  ON_HOLD = 'ON_HOLD',
  CANCELLED = 'CANCELLED'
}

export enum ProductionPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

// Matches: ProductionMaterialDTO.java — field names corrected to match the real
// backend response (previously requiredQuantity/issuedQuantity/consumedQuantity/
// wastedQuantity, none of which exist on the actual DTO).
export interface ProductionMaterial {
  id: number;
  partId: number;
  partName: string;
  partNumber?: string | null;
  batchNumber?: string | null;
  quantityRequired: number;
  quantityConsumed: number | null;
  quantityWasted: number | null;
  status: 'PENDING' | 'CONSUMED' | 'RETURNED' | 'SCRAPPED';
  remarks?: string | null;
  // True once this BOM line's last routing stage completes with output > 0 —
  // drives the final-assembly gate.
  readyForAssembly?: boolean | null;
}

// Matches: StageLogResponseDTO.java — field names corrected to match the real
// backend response (previously operatorId/machineId/startTime/endTime/comments,
// none of which exist on the actual DTO).
export type OperationType = 'PRODUCTION' | 'QA_INSPECTION';

export interface ProductionStageLog {
  id: number;
  productionMaterialId: number | null; // null only for the order-level Final Assembly stage
  sequenceNumber: number | null;
  attemptNumber: number | null;
  routingOperationId: number | null;
  operationType: OperationType | null; // null only for the order-level Final Assembly stage
  stageName: string;
  stageCode?: string | null;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  quantityIn: number | null;
  quantityOut: number | null;
  scrapCount: number | null;
  operatorName?: string | null;
  remarks?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  loggedAt: string;
}

export interface ProductionOrder {
  id: number;
  poNumber: string;
  productName: string;
  productCode?: string;
  aircraftType?: string;
  woNumber?: string;
  workOrderId?: number | null;
  plannedQuantity: number;
  producedQuantity: number;
  rejectedQuantity: number;
  status: ProductionOrderStatus;
  priority: ProductionPriority;
  plannedStartDate?: string;
  plannedEndDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  currentStage?: string;
  productionLine?: string;
  assignedTeam?: string;
  supervisorName?: string;
  qualityStandard?: string;
  qualityNotes?: string;
  qualityPassed?: boolean | null;
  batchNumber?: string;
  remarks?: string;
  closureNotes?: string;
  createdAt: string;
  updatedAt: string;
  completionPercentage?: number;
  materials: ProductionMaterial[];
  stageLogs: ProductionStageLog[];
  outputPartId?: number | null;
  outputPartNumber?: string | null;
}

// ============================================================
// Matches: StartStageRequestDTO / CompleteStageRequestDTO (production-service)
// ============================================================
export interface StartStageRequest {
  operatorName: string;
}

// A PRODUCTION-type stage just reports quantity produced — no accept/reject decision.
export interface CompleteProductionStageRequest {
  quantityProduced: number;
  operatorName?: string;
  remarks?: string;
}

// A QA_INSPECTION-type stage — the good/rework/scrap decision.
export interface CompleteStageRequest {
  quantityGood: number;
  quantityRework: number;
  quantityScrap: number;
  inspectedBy?: string;
  remarks?: string;
}

// ============================================================
// Matches: ShopFloorNcrResponseDTO (production-service) — an in-process reject,
// deliberately separate from inventory-service's vendor-facing NCR system.
// ============================================================
export interface ShopFloorNcr {
  id: number;
  productionOrderId: number;
  productionMaterialId: number | null;
  partName?: string | null;
  stageLogId: number | null;
  stageName?: string | null;
  rejectedQuantity: number;
  disposition: 'SCRAP' | 'REWORK' | 'FINISHED_GOODS_REJECT';
  reasonCode?: string | null;
  description?: string | null;
  reportedBy?: string | null;
  reportedAt: string;
  reworkStageLogId?: number | null;
}

// ============================================================
// Matches: RoutingOperationDTO / RoutingTemplateRequestDTO / RoutingTemplateResponseDTO
// (production-service) — the ordered sequence of manufacturing operations a
// part/product code must pass through.
// ============================================================
export interface RoutingOperation {
  id?: number;
  sequenceNumber: number;
  operationName: string;
  operationType: OperationType;
  operationCode?: string;
  expectedDurationMinutes?: number | null;
  workstation?: string | null;
  instructions?: string | null;
  // Loose cross-service references into production-resource-service — see Machine/Employee.
  preferredMachineId?: number | null;
  preferredMachineCode?: string | null;
  preferredEmployeeId?: number | null;
  preferredEmployeeName?: string | null;
}

export interface RoutingTemplateRequest {
  partNumber: string;
  name: string;
  description?: string;
  isActive?: boolean;
  operations: RoutingOperation[];
}

export interface RoutingTemplate {
  id: number;
  partNumber: string;
  name: string;
  description?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string | null;
  operations: RoutingOperation[];
}

export interface DashboardStats {
  totalOrders: number;
  plannedOrders: number;
  inProductionOrders: number;
  qualityCheckOrders: number;
  completedOrders: number;
  onHoldOrders: number;
  cancelledOrders: number;
  totalPlannedQty: number;
  totalProducedQty: number;
  totalRejectedQty: number;
  overallCompletionRate: number;
  overallRejectionRate: number;
}

export interface ApiResponse<T> {
  status: string;
  message: string;
  data: T;
  timestamp: string;
  errors: string[] | null;
}
