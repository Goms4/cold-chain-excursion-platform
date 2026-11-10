import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Shipments from './pages/Shipments';
import ShipmentDetail from './pages/ShipmentDetail';
import ExcursionDetail from './pages/ExcursionDetail';
import Investigation from './pages/Investigation';
import AuditHistory from './pages/AuditHistory';
import AdminConfig from './pages/AdminConfig';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="shipments" element={<Shipments />} />
          <Route path="shipments/:id" element={<ShipmentDetail />} />
          <Route path="excursions" element={<Dashboard />} />
          <Route path="excursions/:id" element={<ExcursionDetail />} />
          <Route path="investigations" element={<Investigation />} />
          <Route path="audit" element={<AuditHistory />} />
          <Route path="admin" element={<AdminConfig />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
