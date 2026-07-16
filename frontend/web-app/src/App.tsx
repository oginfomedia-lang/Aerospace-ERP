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
import OrgSettings from './pages/OrgSettings';
import VendorInvoices from './pages/VendorInvoices';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import { ProtectedRoute } from './components/ProtectedRoute';
import DebitNotes from './pages/DebitNotes';
import ApprovalMatrix from './pages/ApprovalMatrix';

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

        {/* Protected routes (with Layout) */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/inventory" element={<InventoryList />} />
            <Route path="/inventory/:id/transactions" element={<TransactionHistory />} />
            <Route path="/qa" element={<QAInspection />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/work-orders" element={<WorkOrders />} />
            <Route path="/production" element={<ProductionOrders />} />
            <Route path="/routing-templates" element={<RoutingTemplates />} />
            <Route path="/suppliers" element={<ProcurementSuppliers />} />
            <Route path="/locations" element={<Locations />} />
            <Route path="/purchase-orders" element={<ProcurementOrders />} />
            <Route path="/vendor-invoices" element={<VendorInvoices />} />
            <Route path="/material-receipts" element={<MaterialReceipts />} />
            <Route path="/ncr" element={<NCRPage />} />
            <Route path="/org-settings" element={<OrgSettings />} />
            <Route path="/debit-notes" element={<DebitNotes />} />
            <Route path="/approval-matrix" element={<ApprovalMatrix />} />
          </Route>
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;