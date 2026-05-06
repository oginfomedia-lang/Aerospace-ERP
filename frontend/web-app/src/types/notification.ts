// types/notification.ts
// Maps directly to NotificationLog entity and NotificationType / NotificationStatus enums

export type NotificationSeverity = 'SUCCESS' | 'WARNING' | 'ERROR' | 'INFO';

export type NotificationType =
  | 'LOW_STOCK'
  | 'QA_APPROVED'
  | 'QA_REJECTED'
  | 'QA_PENDING'
  | 'BATCH_EXPIRED'
  | 'STOCK_RECEIVED'
  | 'EXPIRY_WARNING'
  | 'MATERIAL_ISSUED'
  | 'PART_CREATED';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationLog {
  id: number;
  type: NotificationType;
  partId: number | null;
  partName: string | null;
  batchNumber: string | null;
  message: string;
  status: NotificationStatus;
  websocketSent: boolean;
  emailSent: boolean;
  recipient: string | null;
  errorMessage: string | null;
  inspectorName: string | null;
  location: string | null;
  severity: NotificationSeverity;
  createdAt: string;    // ISO datetime
  sentAt: string | null;
}

export interface NotificationApiResponse<T> {
  status: string;
  data: T;
}

// Sidebar stats derived from the feed — computed client-side
export interface NotificationStats {
  total: number;
  warnings: number;
  errors: number;
  success: number;
  info: number;
  failed: number;
}