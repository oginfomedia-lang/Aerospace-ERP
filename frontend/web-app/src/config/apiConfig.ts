// Central place for every backend service's base URL. Each resolves from a
// REACT_APP_* env var (set at build time — CRA only exposes vars prefixed
// REACT_APP_ into the bundle) with the current localhost port as a fallback,
// so local dev needs no setup and a real deployment only needs these env vars
// set at build time instead of editing source.
export const API_URLS = {
  auth: process.env.REACT_APP_AUTH_API_URL || 'http://localhost:8087/api/auth',
  inventory: process.env.REACT_APP_INVENTORY_API_URL || 'http://localhost:8081/api/v1',
  finance: process.env.REACT_APP_FINANCE_API_URL || 'http://localhost:8092/api/v1',
  sales: process.env.REACT_APP_SALES_API_URL || 'http://localhost:8091/api/v1',
  qa: process.env.REACT_APP_QA_API_URL || 'http://localhost:8082/api/v1',
  production: process.env.REACT_APP_PRODUCTION_API_URL || 'http://localhost:8085/api/v1',
  notification: process.env.REACT_APP_NOTIFICATION_API_URL || 'http://localhost:8083/api/v1',
  productionResource: process.env.REACT_APP_PRODUCTION_RESOURCE_API_URL || 'http://localhost:8090/api/v1',
  procurement: process.env.REACT_APP_PROCUREMENT_API_URL || 'http://localhost:8086/api',
  workOrder: process.env.REACT_APP_WORK_ORDER_API_URL || 'http://localhost:8084/api/v1',
};
