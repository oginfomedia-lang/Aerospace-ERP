import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import InventoryList from './pages/InventoryList';
import TransactionHistory from './pages/TransactionHistory';
import QAInspection from './pages/QAInspection';
import Notifications from './pages/Notifications';
import WorkOrders from './pages/WorkOrders';
import ProductionOrders from './pages/ProductionOrders';
import RoutingTemplates from './pages/RoutingTemplates';
import ProcurementSuppliers from './pages/ProcurementSuppliers';
import Locations from './pages/Locations';
import ProcurementOrders from './pages/ProcurementOrders';
import MaterialReceipts from './pages/MaterialReceipts';
import NCRPage from './pages/NCRPage';
import CalibrationPage from './pages/CalibrationPage';
import MachineMaintenancePage from './pages/MachineMaintenancePage';
import EmployeeDirectoryPage from './pages/EmployeeDirectoryPage';
import ToolingRegistryPage from './pages/ToolingRegistryPage';
import SchedulingPage from './pages/SchedulingPage';
import OrgSettings from './pages/OrgSettings';
import VendorInvoices from './pages/VendorInvoices';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import { ProtectedRoute } from './components/ProtectedRoute';
import DebitNotes from './pages/DebitNotes';
import ApprovalMatrix from './pages/ApprovalMatrix';
import PurchaseRequisitions from './pages/PurchaseRequisitions';
import Rfqs from './pages/Rfqs';
import RfqDetail from './pages/RfqDetail';
import SupplierLogin from './pages/supplier/SupplierLogin';
import SupplierActivate from './pages/supplier/SupplierActivate';
import SupplierDashboard from './pages/supplier/SupplierDashboard';
import SupplierRfqDetail from './pages/supplier/SupplierRfqDetail';
import FaiRecords from './pages/FaiRecords';
import InspectionRecords from './pages/InspectionRecords';
import CapaPage from './pages/CapaPage';
import MasterDataReports from './pages/MasterDataReports';
import ApprovalReports from './pages/ApprovalReports';
import AuditTrail from './pages/AuditTrail';
import SlaTracking from './pages/SlaTracking';
import UserManagement from './pages/UserManagement';

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Router>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            borderRadius: '12px',
            fontWeight: '600',
            fontSize: '13px',
          },
        }}
      />
      <Routes>
        {/* Public routes (no Layout) */}
        <Route path="/login" element={!isAuthenticated ? <Login /> : <Navigate to="/dashboard" replace />} />
        <Route path="/register" element={!isAuthenticated ? <Register /> : <Navigate to="/dashboard" replace />} />
        <Route path="/forgot-password" element={!isAuthenticated ? <ForgotPassword /> : <Navigate to="/dashboard" replace />} />
        <Route path="/reset-password" element={!isAuthenticated ? <ResetPassword /> : <Navigate to="/dashboard" replace />} />
        <Route path="/supplier/login" element={<SupplierLogin />} />
        <Route path="/supplier/activate" element={<SupplierActivate />} />
        <Route element={<ProtectedRoute requiredRoles={['SUPPLIER']} />}>
          <Route path="/supplier/dashboard" element={<SupplierDashboard />} />
          <Route path="/supplier/rfqs/:id" element={<SupplierRfqDetail />} />
        </Route>

        {/* Protected routes (with Layout) */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/notifications" element={<Notifications />} />

            {/* Inventory — visible to warehouse, procurement and production roles */}
            <Route element={<ProtectedRoute requiredRoles={['STORE_KEEPER', 'PROCUREMENT_OFFICER', 'PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/inventory" element={<InventoryList />} />
              <Route path="/inventory/:id/transactions" element={<TransactionHistory />} />
              <Route path="/material-receipts" element={<MaterialReceipts />} />
              <Route path="/locations" element={<Locations />} />
            </Route>

            {/* QA / FAI / Calibration */}
            <Route element={<ProtectedRoute requiredRoles={['QA_INSPECTOR', 'MANAGER', 'ADMIN']} />}>
              <Route path="/qa" element={<QAInspection />} />
              <Route path="/fai" element={<FaiRecords />} />
              <Route path="/calibration" element={<CalibrationPage />} />
              <Route path="/inspection-records" element={<InspectionRecords />} />
            </Route>

            {/* NCR — also visible to store keepers who log the non-conformance */}
            <Route element={<ProtectedRoute requiredRoles={['QA_INSPECTOR', 'STORE_KEEPER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/ncr" element={<NCRPage />} />
            </Route>

            <Route element={<ProtectedRoute requiredRoles={['QA_INSPECTOR', 'MANAGER', 'ADMIN']} />}>
              <Route path='/capa' element={<CapaPage />} />
              <Route path="/sla-tracking" element={<SlaTracking />} />
            </Route>

            {/* Production / Work Orders / Routing Templates */}
            <Route element={<ProtectedRoute requiredRoles={['PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/work-orders" element={<WorkOrders />} />
              <Route path="/production" element={<ProductionOrders />} />
              <Route path="/routing-templates" element={<RoutingTemplates />} />
            </Route>

            {/* Machine & Employee Scheduling (advance reservations) — separate guard,
                PRODUCTION_SUPERVISOR needs it but isn't in the Production group above */}
            <Route element={<ProtectedRoute requiredRoles={['PRODUCTION_SUPERVISOR', 'PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/scheduling" element={<SchedulingPage />} />
            </Route>

            {/* Machine registry & maintenance — own guard, different roles from Production */}
            <Route element={<ProtectedRoute requiredRoles={['PRODUCTION_SUPERVISOR', 'MAINTENANCE_ENGINEER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/machines" element={<MachineMaintenancePage />} />
              <Route path="/tooling" element={<ToolingRegistryPage />} />
            </Route>

            {/* Employee & Skill registry — roles match who can write to it; PRODUCTION_ENGINEER
                gets API read-access for the Routing Template picker but not this nav page. */}
            <Route element={<ProtectedRoute requiredRoles={['PRODUCTION_SUPERVISOR', 'MANAGER', 'ADMIN']} />}>
              <Route path="/employees" element={<EmployeeDirectoryPage />} />
            </Route>

            {/* Procurement */}
            {/* Suppliers gets its own guard (not the shared Procurement one below) because
                QA_INSPECTOR needs to reach it to confirm/reject new suppliers, but must not
                gain access to POs/invoices/RFQs/debit notes just from that. */}
            <Route element={<ProtectedRoute requiredRoles={['PROCUREMENT_OFFICER', 'QA_INSPECTOR', 'MANAGER', 'ADMIN']} />}>
              <Route path="/suppliers" element={<ProcurementSuppliers />} />
            </Route>
            <Route element={<ProtectedRoute requiredRoles={['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']} />}>
              <Route path="/purchase-orders" element={<ProcurementOrders />} />
              <Route path="/vendor-invoices" element={<VendorInvoices />} />
              <Route path="/purchase-requisitions" element={<PurchaseRequisitions />} />
              <Route path="/rfqs" element={<Rfqs />} />
              <Route path="/rfqs/:id" element={<RfqDetail />} />
              <Route path="/debit-notes" element={<DebitNotes />} />
            </Route>

            {/* Org Settings / Approval Matrix — management only */}
            <Route element={<ProtectedRoute requiredRoles={['MANAGER', 'ADMIN']} />}>
              <Route path="/org-settings" element={<OrgSettings />} />
              <Route path="/approval-matrix" element={<ApprovalMatrix />} />
              <Route path="/master-data-reports" element={<MasterDataReports />} />
              <Route path="/approval-reports" element={<ApprovalReports />} />
              <Route path="/audit-trail" element={<AuditTrail />} />
            </Route>

            {/* User Management — Admin only, not Manager */}
            <Route element={<ProtectedRoute requiredRoles={['ADMIN']} />}>
              <Route path="/admin/users" element={<UserManagement />} />
            </Route>
          </Route>
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;