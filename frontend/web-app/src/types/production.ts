export enum ProductionOrderStatus {
  PLANNED = 'PLANNED',
  RELEASED = 'RELEASED',
  IN_PRODUCTION = 'IN_PRODUCTION',
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

export interface ProductionMaterial {
  id: number;
  partId: number;
  partName: string;
  requiredQuantity: number;
  issuedQuantity: number;
  consumedQuantity: number;
  wastedQuantity: number;
  status: string;
}

export interface ProductionStageLog {
  id: number;
  stageName: string;
  status: string;
  operatorId: string;
  machineId: string;
  startTime: string;
  endTime?: string;
  comments?: string;
}

export interface ProductionOrder {
  id: number;
  poNumber: string;
  productName: string;
  productCode?: string;
  aircraftType?: string;
  woNumber?: string;
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
  batchNumber?: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
  materials: ProductionMaterial[];
  stageLogs: ProductionStageLog[];
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
