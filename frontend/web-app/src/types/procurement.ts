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

  // Structured address + tax registration — shown on the printed PO's Vendor
  // Address block, and used to decide CGST+SGST vs IGST when computing PO tax.
  city?: string | null;
  state?: string | null;
  stateCode?: string | null;
  pincode?: string | null;
  country?: string | null;
  gstin?: string | null;
  pan?: string | null;
  gstinRegType?: string | null;

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
  city?: string;
  state?: string;
  stateCode?: string;
  pincode?: string;
  country?: string;
  gstin?: string;
  pan?: string;
  gstinRegType?: string;
  certificationStatus: string;
  rating?: string;
}

export interface POLineItemDTO {
  skuCode: string;
  quantity: number;
  unitPrice: number;
  totalPrice?: number;
  uom: string;

  // Snapshotted from the Part when the SKU is selected — read-only, not negotiated per PO
  hsnCode?: string | null;
  taxRatePercent?: number | null;

  // QA outcome (read-only, populated once the received goods for this SKU are inspected)
  acceptedQuantity?: number | null;
  rejectedQuantity?: number | null;
  ncrNumber?: string | null;
}

export interface PurchaseOrderRequest {
  supplierId: number;
  deliveryDate: string;
  currency: string;
  remarks: string;
  shipToName?: string;
  shipToAddress?: string;
  shipToState?: string;
  shipToStateCode?: string;
  paymentTerms?: string;
  payment?: string;
  deliveryTerms?: string;
  exciseDuty?: string;
  termsAndConditions?: string;
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
  orderDate: string;
  deliveryDate: string;
  items: POLineItemDTO[];
  remarks: string | null;

  // Set only when this PO was auto-raised for a Work Order material shortage
  sourceWorkOrderId?: number | null;
  sourceWoNumber?: string | null;

  // Optional per-PO overrides for the printed document — null falls back to the
  // Organization Profile default at render time.
  shipToName?: string | null;
  shipToAddress?: string | null;
  shipToState?: string | null;
  shipToStateCode?: string | null;
  paymentTerms?: string | null;
  payment?: string | null;
  deliveryTerms?: string | null;
  exciseDuty?: string | null;
  termsAndConditions?: string | null;

  // Approval chain (PUR-BR-001)
  currentApprovalLevel: number | null;
  documentVersion: number;
  pendingApproverRole?: string | null;

  // PUR-BR-005: non-blocking warning surfaced on create
  duplicateWarning?: string | null;
}

// ============================================================
// Matches: OrganizationProfile.java / OrganizationProfileRequest.java (procurement-service)
// Singleton "who are we" record used as the letterhead + defaults on printed POs.
// ============================================================
export interface OrganizationProfile {
  id: number;
  companyName: string | null;
  addressLine: string | null;
  city: string | null;
  state: string | null;
  stateCode: string | null;
  pincode: string | null;
  country: string | null;
  phone: string | null;
  telefax: string | null;
  email: string | null;

  gstin: string | null;
  pan: string | null;

  range: string | null;
  division: string | null;

  bankName: string | null;
  bankBranch: string | null;
  bankAccountNumber: string | null;
  bankIfsc: string | null;

  defaultPaymentTerms: string | null;
  defaultPayment: string | null;
  defaultDeliveryTerms: string | null;
  defaultExciseDuty: string | null;
  termsAndConditions: string | null;
}

export type OrganizationProfileRequest = Omit<OrganizationProfile, 'id'>;

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
