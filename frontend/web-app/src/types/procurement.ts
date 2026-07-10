export interface Supplier {
  id: number;
  name: string;
  vendorCode: string;
  contactPerson?: string;
  // NOTE: matches backend field names (email/phone) — the request used to send
  // contactEmail/contactPhone which the backend silently ignored (unknown JSON keys).
  email: string;
  phone: string;
  address?: string;
  certificationStatus: string;
  rating: string | null;
  active: boolean;
}

export interface SupplierRequest {
  name: string;
  vendorCode: string;
  contactPerson?: string;
  email: string;
  phone: string;
  address?: string;
  certificationStatus: string;
  rating?: string;
}

export interface POLineItemDTO {
  skuCode: string;
  quantity: number;
  unitPrice: number;
  totalPrice?: number;
  uom: string;
}

export interface PurchaseOrderRequest {
  supplierId: number;
  deliveryDate: string;
  currency: string;
  remarks: string;
  items: POLineItemDTO[];
}

export interface PurchaseOrderResponse {
  id: number;
  poNumber: string;
  supplierId: number;
  supplierName: string;
  status: string;
  totalAmount: number;
  currency: string;
  deliveryDate: string;
  items: POLineItemDTO[];
  remarks: string | null;

  // Approval chain (PUR-BR-001)
  currentApprovalLevel: number | null;
  documentVersion: number;
  pendingApproverRole?: string | null;

  // PUR-BR-005: non-blocking warning surfaced on create
  duplicateWarning?: string | null;
}

export interface POApprovalActionRequest {
  remarks?: string;
}

export interface POApprovalHistoryEntry {
  approvalLevel: number;
  approverRole: string;
  status: 'WAITING' | 'PENDING' | 'APPROVED' | 'REJECTED';
  approverUsername: string | null;
  approvedAt: string | null;
  remarks: string | null;
  digitalSignatureHash: string | null;
  documentVersion: number;
}
