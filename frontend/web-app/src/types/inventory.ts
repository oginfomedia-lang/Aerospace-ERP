// ============================================================
// Matches: PartResponseDTO.java
// ============================================================
export interface Part {
  id: number;
  name: string;
  serialNumber: string;
  batchNumber: string | null;
  quantity: number | null;
  location: string | null;
  partType: string | null;
  criticality: string | null;
  reorderLevel: number | null;
  status: string | null;
  qaRemarks: string | null;
  expiryDate: string | null;

  // Planning levels (ITM-BR-006: minStock <= reorderLevel <= maxStock)
  minStock: number | null;
  maxStock: number | null;
  safetyStock: number | null;
  leadTimeDays: number | null;

  // Item lifecycle: ACTIVE, INACTIVE, DISCONTINUED
  itemStatus: string | null;

  // Tracking flags (ITM-BR-002, ITM-BR-003, GRN-FR-003)
  batchRequired: boolean;
  serialRequired: boolean;
  expiryTracking: boolean;
  shelfLifeDays: number | null;
  qcRequired: boolean;

  // Set only on GRN receipt responses (system-generated, GRN-FR-001)
  grnNumber?: string | null;

  // Authoritative per-batch/status breakdown — a part can have APPROVED, PENDING_QA,
  // RESERVED and REJECTED stock at the same time, across different batches. The
  // fields above (batchNumber/quantity/status/location/expiryDate) are only ever an
  // APPROVED-stock summary; anything needing to see non-APPROVED stock (e.g. the QA
  // queue) must read this list instead.
  balances: StockBalance[];
}

// ============================================================
// Matches: StockBalanceDTO.java
// ============================================================
export interface StockBalance {
  batchNumber: string;
  status: string;
  quantity: number;
  location: string | null;
  expiryDate: string | null;
}

// ============================================================
// Matches: PartRequestDTO.java
// ============================================================
export interface PartRequest {
  name: string;
  serialNumber: string;
  batchNumber?: string;
  quantity?: number;
  location?: string;
  partType?: string;
  criticality?: string;
  reorderLevel?: number;
  status?: string;
  qaRemarks?: string;

  minStock?: number;
  maxStock?: number;
  safetyStock?: number;
  leadTimeDays?: number;
  itemStatus?: string;
  batchRequired?: boolean;
  serialRequired?: boolean;
  expiryTracking?: boolean;
  shelfLifeDays?: number;
  qcRequired?: boolean;
}

// ============================================================
// Matches: DashboardResponseDTO.java
// ============================================================
export interface DashboardStats {
  totalParts: number;
  totalQuantity: number;
  lowStockItems: number;
  outOfStockItems: number;
  pendingQAItems: number;
  rejectedItems: number;
  inventoryValue: number;
  partsByType: Record<string, number>;
}

// ============================================================
// Matches: InventoryTransaction.java entity
// ============================================================
export interface InventoryTransaction {
  id: number;
  partId: number;
  batchNumber: string | null;
  transactionType: string;
  quantity: number;
  referenceDoc: string | null;
  remarks: string | null;
  timestamp: string;
}

// ============================================================
// Matches: InspectionRequestDTO.java in qa-service (port 8082)
// ============================================================
export interface InspectionRequest {
  partId: number;
  batchNumber: string;
  decision: 'APPROVED' | 'REJECTED';
  remarks: string;
  certificateNumber?: string;
  inspectorName?: string;
  location?: string;
}

// ============================================================
// Matches: InspectionResponseDTO.java in qa-service
// ============================================================
export interface InspectionReport {
  id: number;
  partId: number;
  batchNumber: string;
  decision: 'APPROVED' | 'REJECTED';
  remarks: string;
  certificateNumber: string | null;
  inspectorName: string | null;
  inspectedAt: string;
  publishedToInventory: boolean;
}

// ============================================================
// Matches: ApiResponse.java — used by BOTH inventory (8081) and qa (8082)
// ============================================================
export interface ApiResponse<T> {
  message: string;
  data: T;
}

export type StockStatus = 'PENDING_QA' | 'APPROVED' | 'REJECTED' | 'RESERVED' | 'EMPTY';