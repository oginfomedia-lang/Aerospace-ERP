// Matches: CustomerRequestDTO.java / CustomerResponseDTO.java (sales-service)
export interface Customer {
  id: number;
  customerCode: string;
  name: string;
  contactPerson?: string | null;
  email?: string | null;
  phone?: string | null;
  billingAddress?: string | null;
  shippingAddress?: string | null;
  currency?: string | null;
  paymentTerms?: string | null;
  status: string; // ACTIVE, INACTIVE, ON_HOLD
  version?: number | null;
  createdAt: string;
}

export interface CustomerRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  billingAddress?: string;
  shippingAddress?: string;
  currency?: string;
  paymentTerms?: string;
  status?: string;
}

// Matches: QuotationLineRequestDTO.java / SalesOrderLineRequestDTO.java — identical shape,
// shared here since both entities' line items are structurally the same.
export interface LineItemRequest {
  partId: number;
  partNumber?: string;
  partName?: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  taxPercent?: number;
}

// Matches: QuotationLineResponseDTO.java
export interface QuotationLine {
  id: number;
  partId: number;
  partNumber?: string | null;
  partName?: string | null;
  quantity: number;
  unitPrice: number;
  discountPercent?: number | null;
  taxPercent?: number | null;
  lineTotal: number;
}

// Matches: QuotationRequestDTO.java
export interface QuotationRequest {
  customerId: number;
  validFrom?: string;
  validUntil?: string;
  currency?: string;
  paymentTerms?: string;
  deliveryTerms?: string;
  lines: LineItemRequest[];
}

// Matches: QuotationResponseDTO.java
export interface Quotation {
  id: number;
  quotationNumber: string;
  customerId: number;
  customerName: string;
  validFrom?: string | null;
  validUntil?: string | null;
  currency?: string | null;
  paymentTerms?: string | null;
  deliveryTerms?: string | null;
  totalAmount: number;
  status: string; // DRAFT, SUBMITTED, APPROVED, REJECTED, EXPIRED, CONVERTED
  convertedToSalesOrderId?: number | null;
  lines: QuotationLine[];
  createdBy?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

// Matches: SalesOrderLineResponseDTO.java
export interface SalesOrderLine {
  id: number;
  partId: number;
  partNumber?: string | null;
  partName?: string | null;
  quantity: number;
  unitPrice: number;
  discountPercent?: number | null;
  taxPercent?: number | null;
  lineTotal: number;
  quantityAllocated: number;
  quantityShipped: number;
}

// Matches: SalesOrderRequestDTO.java — promisedDeliveryDate deliberately absent, only
// ATP is allowed to set it.
export interface SalesOrderRequest {
  customerId: number;
  orderType?: string;
  requestedDeliveryDate?: string;
  shippingAddress?: string;
  priority?: string;
  lines: LineItemRequest[];
}

// Matches: SalesOrderResponseDTO.java
export interface SalesOrder {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  orderType?: string | null;
  requestedDeliveryDate?: string | null;
  promisedDeliveryDate?: string | null;
  shippingAddress?: string | null;
  priority?: string | null;
  status: string; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, CONFIRMED, ALLOCATED,
                   // IN_FULFILLMENT, DISPATCHED, DELIVERED, CLOSED, CANCELLED
  totalAmount: number;
  sourceQuotationId?: number | null;
  revisionNumber: number;
  supersedesId?: number | null;
  isCurrentRevision: boolean;
  lines: SalesOrderLine[];
  createdBy?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

// Matches: ApprovalDecisionDTO.java
export interface ApprovalDecision {
  remarks?: string;
}

// Matches: AtpResultDTO.java
export interface AtpResult {
  salesOrderLineId: number;
  partId: number;
  partNumber?: string | null;
  requestedQuantity: number;
  availableNow: number;
  canFulfillNow: boolean;
  earliestDeliveryDate: string;
  shortageQuantity: number;
}

// Matches: DispatchLineResponseDTO.java
export interface DispatchLine {
  id: number;
  salesOrderLineId: number;
  partId: number;
  partNumber?: string | null;
  quantity: number;
}

// Matches: DispatchResponseDTO.java
export interface Dispatch {
  id: number;
  dispatchNumber: string;
  salesOrderId: number;
  orderNumber: string;
  status: string; // PENDING, PACKED, DISPATCHED
  carrierName?: string | null;
  trackingNumber?: string | null;
  packedBy?: string | null;
  dispatchedBy?: string | null;
  packedAt?: string | null;
  dispatchedAt?: string | null;
  lines: DispatchLine[];
  createdAt: string;
}

// Matches: ConfirmDispatchRequestDTO.java
export interface ConfirmDispatchRequest {
  carrierName: string;
  trackingNumber: string;
}

// Matches: ReturnAuthorizationRequestDTO.java
export interface ReturnAuthorizationRequest {
  salesOrderId: number;
  partId: number;
  partNumber?: string;
  quantity: number;
  reasonCode?: string;
  reasonDescription?: string;
}

// Matches: ReturnAuthorizationResponseDTO.java
export interface ReturnAuthorization {
  id: number;
  rmaNumber: string;
  salesOrderId: number;
  orderNumber: string;
  partId: number;
  partNumber?: string | null;
  quantity: number;
  reasonCode?: string | null;
  reasonDescription?: string | null;
  disposition: string; // PENDING_INSPECTION, ACCEPTED, REJECTED, REPAIR, REPLACE, CREDIT_RECOMMENDED
  receivedBatchNumber?: string | null;
  inspectedBy?: string | null;
  inspectedAt?: string | null;
  inspectionNotes?: string | null;
  requestedBy?: string | null;
  createdAt: string;
}

// Matches: InspectReturnRequestDTO.java
export interface InspectReturnRequest {
  disposition: string;
  notes?: string;
}

// Matches: OrderTrackingDTO.java
export interface OrderTracking {
  salesOrderId: number;
  orderNumber: string;
  status: string;
  dispatches: {
    dispatchNumber: string;
    status: string;
    carrierName?: string | null;
    trackingNumber?: string | null;
    dispatchedAt?: string | null;
  }[];
  allocations: {
    partNumber?: string | null;
    batchNumber: string;
    quantity: number;
    status: string; // ACTIVE, RELEASED, SHIPPED
  }[];
}
