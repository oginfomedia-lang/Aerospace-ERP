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

  // Soft reference to procurement-service's Supplier — used to auto-raise a DRAFT PO
  // when a Work Order needs more of this part than is currently APPROVED in stock.
  preferredSupplierId: number | null;

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
  preferredSupplierId?: number;
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
// Matches: MaterialReceiptItemRequest / MaterialReceiptItemResponse (inventory-service)
// ============================================================
export interface MaterialReceiptItemRequest {
  skuCode: string;
  quantity: number;
  uom: string;
}

export interface MaterialReceiptItem {
  id: number;
  skuCode: string;
  quantity: number;
  uom: string;
  grnNumber: string | null;
}

// ============================================================
// Matches: MaterialReceiptRequest.java (inventory-service)
// ============================================================
export interface MaterialReceiptRequest {
  poNumber: string;
  supplierName: string;
  challanNumber: string;
  transportName: string;
  receivedBy: string;
  items: MaterialReceiptItemRequest[];
}

// ============================================================
// Matches: MaterialReceiptResponse.java (inventory-service)
// ============================================================
export interface MaterialReceipt {
  id: number;
  mrNumber: string;
  poNumber: string | null;
  supplierName: string | null;
  challanNumber: string | null;
  transportName: string | null;
  receivedBy: string | null;
  receivedAt: string;
  status: 'RECEIVED_AT_GATE' | 'GRN_FORMED';
  items: MaterialReceiptItem[];
}

// ============================================================
// Matches: FormGrnRequest.java (inventory-service)
// ============================================================
export interface FormGrnRequest {
  location: string;
  expiryDate?: string;
  inspectionQty?: number;
}

// ============================================================
// Matches: NcrResponse.java (inventory-service) — auto-created on QA rejection
// ============================================================
export interface NonConformanceReport {
  id: number;
  ncrNumber: string;
  partId: number;
  partName: string;
  batchNumber: string;
  quantity: number;
  reason: string | null;
  status: 'OPEN' | 'DISPOSITIONED';
  dispositionType: 'RETURN_TO_VENDOR' | 'SCRAP' | 'USE_AS_IS' | null;
  dispositionRemarks: string | null;
  dispositionedBy: string | null;
  createdAt: string;
  dispositionedAt: string | null;
}

// ============================================================
// Matches: DispositionRequest.java (inventory-service)
// ============================================================
export interface DispositionRequest {
  dispositionType: 'RETURN_TO_VENDOR' | 'SCRAP' | 'USE_AS_IS';
  remarks: string;
  dispositionedBy: string;
}

// ============================================================
// Matches: ApiResponse.java — used by BOTH inventory (8081) and qa (8082)
// ============================================================
export interface ApiResponse<T> {
  message: string;
  data: T;
}

export type StockStatus = 'PENDING_QA' | 'APPROVED' | 'REJECTED' | 'RESERVED' | 'EMPTY';