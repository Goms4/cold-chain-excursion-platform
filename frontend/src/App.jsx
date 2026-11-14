import React, { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';
import Shipments from './pages/Shipments';
import ShipmentDetail from './pages/ShipmentDetail';
import Excursions from './pages/Excursions';
import ExcursionDetail from './pages/ExcursionDetail';
import Investigation from './pages/Investigation';
import AuditHistory from './pages/AuditHistory';
import AdminConfig from './pages/AdminConfig';
import { apiRequest, clearSessionIdentity, getActiveRole, setSessionIdentity } from './api/client';

export default function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [checking, setChecking] = useState(true);
  const [loginMessage, setLoginMessage] = useState('');
  useEffect(() => {
    apiRequest('/api/auth/me').then(user => { setSessionIdentity(user.role, user.displayName); setAuthenticated(true); })
      .catch(() => clearSessionIdentity()).finally(() => setChecking(false));
    const expired = () => { clearSessionIdentity(); setAuthenticated(false); setLoginMessage('Your session ended. Sign in again.'); };
    window.addEventListener('coldlatch:unauthorized', expired);
    return () => window.removeEventListener('coldlatch:unauthorized', expired);
  }, []);
  const signOut = async () => {
    try { await apiRequest('/api/auth/logout', { method: 'POST' }); }
    finally { clearSessionIdentity(); setAuthenticated(false); setLoginMessage(''); }
  };

  return (
    <BrowserRouter>
      {checking ? <main className="auth-page"><span>Loading workspace…</span></main> : <Routes>
        <Route path="/login" element={authenticated ? <Navigate to="/dashboard" replace /> : <AuthPage message={loginMessage} onSignedIn={() => { setAuthenticated(true); setLoginMessage(''); }} />} />
        <Route path="/" element={authenticated ? <Layout role={getActiveRole()} onSignOut={signOut} /> : <Navigate to="/login" replace />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="shipments" element={<Shipments />} />
          <Route path="shipments/:id" element={<ShipmentDetail />} />
          <Route path="issues" element={<Excursions />} />
          <Route path="issues/:id" element={<ExcursionDetail />} />
          <Route path="investigations" element={<Investigation />} />
          <Route path="audit" element={<AuditHistory />} />
          <Route path="simulator" element={<Navigate to="/dashboard" replace />} />
          <Route path="configuration" element={<AdminConfig />} />
          <Route path="config" element={<AdminConfig />} />
          <Route path="admin" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
        <Route path="*" element={<Navigate to={authenticated ? '/dashboard' : '/login'} replace />} />
      </Routes>}
    </BrowserRouter>
  );
}
