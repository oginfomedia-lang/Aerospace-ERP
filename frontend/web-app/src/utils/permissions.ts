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

// Matches CustomerController/QuotationController/SalesOrderController's class-level
// @PreAuthorize (sales-service).
export const CAN_MANAGE_SALES = ['SALES_EXECUTIVE', 'SALES_MANAGER', 'MANAGER', 'ADMIN'];
// Matches SalesOrderController.approve/reject's method-level override.
export const CAN_APPROVE_SALES = ['SALES_MANAGER', 'MANAGER', 'ADMIN'];
// Matches DispatchController's class-level @PreAuthorize.
export const CAN_FULFILL_ORDERS = ['WAREHOUSE_OPERATOR', 'SALES_MANAGER', 'MANAGER', 'ADMIN'];
// Matches ReturnAuthorizationController.inspect's method-level override.
export const CAN_INSPECT_RETURNS = ['QA_INSPECTOR', 'SALES_MANAGER', 'MANAGER', 'ADMIN'];

// Matches AccountController/JournalEntryController/CostCenterController/etc.'s
// class-level @PreAuthorize (finance-service) — day-to-day journal creation/submission.
export const CAN_MANAGE_FINANCE = ['ACCOUNTANT', 'FINANCE_MANAGER', 'MANAGER', 'ADMIN'];
// Matches JournalEntryController.approve/reject/reverse and AccountingPeriodController
// close/reopen's method-level override — approvals, period close/reopen, CoA/tax/
// posting-rule/currency configuration.
export const CAN_APPROVE_FINANCE = ['FINANCE_MANAGER', 'MANAGER', 'ADMIN'];
// Matches AccountingPeriodController.lock/unlock's method-level override — locking a
// period permanently is intentionally rarer/higher-privilege than close/reopen, ADMIN only.
export const CAN_LOCK_PERIOD = ['ADMIN'];

export function hasAnyRole(userRoles: string[] | undefined | null, allowed: string[]): boolean {
  if (!userRoles || userRoles.length === 0) return false;
  return allowed.some((role) => userRoles.includes(role));
}
