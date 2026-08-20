import axios from 'axios';
import { tokenStorage } from '../utils/tokenStorage';

// ApprovalMatrixController lives in common-lib, which is compiled into EVERY service —
// so each service that uses it (procurement, sales, finance, ...) has its OWN
// independent approval_matrix table in its OWN database. There is no single shared
// table; the Approval Matrix page and its create/edit modal both need to know which
// service's rules they're looking at.
//
// That common-lib controller is mapped at "/api/approval-matrix" (no version prefix),
// unlike every other endpoint in these services which lives under "/api/v1/...". Using
// each service's normal (versioned) axios instance here would silently 403 — the
// request never matches any controller, and falls through to Spring Security's
// catch-all "authenticated" rule, which rejects it before it ever reaches a handler.
// So this file builds its own unversioned clients instead of reusing salesAxios /
// procurementAxios / financeAxios.
function makeApprovalApi(origin: string) {
  const api = axios.create({ baseURL: `${origin}/api` });
  api.interceptors.request.use((config) => {
    const token = tokenStorage.getAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });
  return api;
}

export const APPROVAL_SERVICES = {
  procurement: { label: 'Procurement (Purchase Orders, Vendor Invoices)', api: makeApprovalApi('http://localhost:8086'), exampleModule: 'PURCHASE_ORDER' },
  sales: { label: 'Sales (Sales Orders)', api: makeApprovalApi('http://localhost:8091'), exampleModule: 'SALES_ORDER' },
  finance: { label: 'Finance (Journal Entries)', api: makeApprovalApi('http://localhost:8092'), exampleModule: 'JOURNAL_ENTRY' },
} as const;

export type ServiceKey = keyof typeof APPROVAL_SERVICES;
