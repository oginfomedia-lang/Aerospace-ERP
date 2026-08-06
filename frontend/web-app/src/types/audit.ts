// Matches: AuditLog.java (common-lib) — same shape returned by both
// inventory-service and procurement-service, since AuditLogController lives in common-lib
export interface AuditLogEntry {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  actor: string;
  ipAddress: string | null;
  previousValue: string | null;
  newValue: string | null;
  timestamp: string;
}

export type AuditServiceKey = 'inventory' | 'procurement' | 'qa' | 'order' | 'production';

export interface AuditLogRow extends AuditLogEntry {
  service: AuditServiceKey;
}
