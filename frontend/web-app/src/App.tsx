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
import ProcurementSuppliers from './pages/ProcurementSuppliers';
import ProcurementOrders from './pages/ProcurementOrders';

function App() {
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
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/inventory" element={<InventoryList />} />
          <Route path="/inventory/:id/transactions" element={<TransactionHistory />} />

          {/* QA Service route — talks to port 8082 */}
          <Route path="/qa" element={<QAInspection />} />

          <Route path='/notifications' element={<Notifications />} />

          <Route path='/work-orders' element={<WorkOrders />} />

          {/* Production service */}
          <Route path='/production' element={<ProductionOrders />} />

          {/* Procurement service */}
          <Route path="/suppliers" element={<ProcurementSuppliers />} />
          <Route path="/purchase-orders" element={<ProcurementOrders />} />

          {/* Catch all */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
