// Frontend RBAC constants — mirrors the @PreAuthorize role lists on the backend
// controllers 1:1 (see PartController, PurchaseOrderController, NcrController,
// CapaController, SupplierController, QAController/CalibrationController/
// FaiController/CocController, ProductionOrderController, WorkOrderController).
// Single source of truth so role strings aren't hand-typed/drifting across pages.

export const CAN_MANAGE_PARTS = ['STORE_KEEPER', 'MANAGER', 'ADMIN'];
export const CAN_ISSUE_MATERIAL = ['STORE_KEEPER', 'PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN'];
export const CAN_QA_DECISION = ['QA_INSPECTOR', 'MANAGER', 'ADMIN'];
export const CAN_DISPOSITION_NCR = ['QA_INSPECTOR', 'MANAGER', 'ADMIN'];
export const CAN_MANAGE_CAPA = ['QA_INSPECTOR', 'MANAGER', 'ADMIN'];
export const CAN_MANAGE_PROCUREMENT = ['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN'];
export const CAN_APPROVE_PROCUREMENT = ['MANAGER', 'ADMIN'];
export const CAN_MANAGE_ORG_SETTINGS = ['MANAGER', 'ADMIN'];
export const CAN_MANAGE_QA = ['QA_INSPECTOR', 'MANAGER', 'ADMIN'];
export const CAN_MANAGE_PRODUCTION = ['PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN'];
export const CAN_CONFIRM_SUPPLIERS = ['QA_INSPECTOR', 'MANAGER', 'ADMIN'];

export function hasAnyRole(userRoles: string[] | undefined | null, allowed: string[]): boolean {
  if (!userRoles || userRoles.length === 0) return false;
  return allowed.some((role) => userRoles.includes(role));
}
