// types/workOrder.ts
// Maps directly to WorkOrderDTOs.java, WorkOrderStatus.java, Priority.java

export type WorkOrderStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type LineItemStatus = 'PENDING' | 'PARTIALLY_ISSUED' |  'ISSUED' | 'RETURNED' | 'CANCELLED';

export interface MaterialLineItem {
  id: number;
  partId: number;
  partName: string | null;
  partNumber: string | null;
  batchNumber: string | null;
  quantityRequired: number;
  quantityIssued: number;
  status: LineItemStatus;
  remarks: string | null;
  // Snapshotted from the Part item master when this line was added.
  unitPrice: number | null;
  taxRatePercent: number | null;
  description: string | null;
}

export interface WorkOrder {
  id: number;
  woNumber: string;
  aircraftId: string;
  aircraftType: string | null;
  taskDescription: string;
  taskCode: string | null;
  status: WorkOrderStatus;
  priority: Priority;
  assignedTo: string | null;
  createdBy: string | null;
  location: string | null;
  dueDate: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
  remarks: string | null;
  closureNotes: string | null;
  materialLineItems: MaterialLineItem[];
  totalPartsRequired: number;
  totalPartsIssued: number;
  allMaterialReady: boolean;
  totalMaterialCost: number | null;
}

export interface StatusHistoryEntry {
  id: number;
  woNumber: string;
  fromStatus: WorkOrderStatus | null;
  toStatus: WorkOrderStatus;
  changedBy: string | null;
  remarks: string | null;
  changedAt: string;
}

export interface DashboardStats {
  totalWorkOrders: number;
  draft: number;
  open: number;
  inProgress: number;
  onHold: number;
  completed: number;
  cancelled: number;
  criticalPriority: number;
  overdueCount: number;
}

// ── Request types ─────────────────────────────────────────────────────────────

export interface CreateWorkOrderRequest {
  aircraftId: string;
  aircraftType?: string;
  taskDescription: string;
  taskCode?: string;
  priority: Priority;
  assignedTo?: string;
  createdBy?: string;
  location?: string;
  dueDate?: string;
  remarks?: string;
  materialItems?: MaterialLineItemRequest[];
}

export interface MaterialLineItemRequest {
  partId: number;
  partName?: string;
  partNumber?: string;
  quantityRequired: number;
  remarks?: string;
  unitPrice?: number;
  taxRatePercent?: number;
  description?: string;
}

export interface UpdateWorkOrderRequest {
  taskDescription?: string;
  taskCode?: string;
  priority?: Priority;
  assignedTo?: string;
  location?: string;
  dueDate?: string;
  remarks?: string;
}

export interface StatusTransitionRequest {
  newStatus: WorkOrderStatus;
  changedBy?: string;
  remarks?: string;
}

export interface IssueMaterialRequest {
  lineItemId: number;
  quantityIssued: number;
  batchNumber?: string;
  issuedBy?: string;
}

export interface ApiResponse<T> {
  status: string;
  message: string | null;
  data: T;
}