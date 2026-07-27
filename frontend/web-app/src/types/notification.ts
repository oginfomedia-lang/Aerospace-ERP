// types/notification.ts
// Maps directly to NotificationLog entity and NotificationType / NotificationStatus enums

export type NotificationSeverity = 'SUCCESS' | 'WARNING' | 'ERROR' | 'INFO';

export type NotificationType =
  // ── Inventory types ────────────────────────────────────────────────────────
  | 'LOW_STOCK'
  | 'QA_APPROVED'
  | 'QA_REJECTED'
  | 'QA_PENDING'
  | 'BATCH_EXPIRED'
  | 'STOCK_RECEIVED'
  | 'EXPIRY_WARNING'
  | 'MATERIAL_ISSUED'
  | 'PART_CREATED'
  // ── Work order types ───────────────────────────────────────────────────────
  | 'WORK_ORDER_CREATED'
  | 'WORK_ORDER_UPDATED'
  | 'WORK_ORDER_COMPLETED'
  // ── QA types ───────────────────────────────────────────────────────────────
  | 'CALIBRATION_DUE';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationLog {
  id: number;
  type: NotificationType;
  partId: number | null;       // also holds workOrderId for WO notifications
  partName: string | null;     // also holds woNumber for WO notifications
  batchNumber: string | null;  // also holds aircraftId for WO notifications
  message: string;
  status: NotificationStatus;
  websocketSent: boolean;
  emailSent: boolean;
  recipient: string | null;
  errorMessage: string | null;
  inspectorName: string | null;
  location: string | null;
  severity: NotificationSeverity;
  createdAt: string;
  sentAt: string | null;
}

export interface NotificationApiResponse<T> {
  status: string;
  data: T;
}

export interface NotificationStats {
  total: number;
  warnings: number;
  errors: number;
  success: number;
  info: number;
  failed: number;
}