export interface Supplier {
  id: number;
  name: string;
  vendorCode: string;
  contactEmail: string;
  contactPhone: string;
  certificationStatus: string;
  rating: number;
}

export interface SupplierRequest {
  name: string;
  vendorCode: string;
  contactEmail: string;
  contactPhone: string;
  certificationStatus: string;
  rating: number;
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
  supplierName: string;
  status: string;
  totalAmount: number;
  currency: string;
  deliveryDate: string;
  items: POLineItemDTO[];
}
