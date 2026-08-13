import procurementApi from '../api/procurementAxios';
import salesApi from '../api/salesAxios';

// ApprovalMatrixController lives in common-lib, which is compiled into EVERY service —
// so each service that uses it (procurement, sales, ...) has its OWN independent
// approval_matrix table in its OWN database. There is no single shared table; the
// Approval Matrix page and its create/edit modal both need to know which service's
// rules they're looking at.
export const APPROVAL_SERVICES = {
  procurement: { label: 'Procurement (Purchase Orders, Vendor Invoices)', api: procurementApi, exampleModule: 'PURCHASE_ORDER' },
  sales: { label: 'Sales (Sales Orders)', api: salesApi, exampleModule: 'SALES_ORDER' },
} as const;

export type ServiceKey = keyof typeof APPROVAL_SERVICES;
