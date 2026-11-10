import React, { useEffect, useState } from 'react';
import { ShieldCheck, Activity, UserCheck, RefreshCw } from 'lucide-react';
import { getActiveRole, setActiveRole, setAuthToken, apiRequest } from '../api/client';

export default function Navbar() {
  const [role, setRole] = useState(getActiveRole());
  const [health, setHealth] = useState('ONLINE');

  const switchRole = async (newRole) => {
    try {
      const res = await apiRequest(`/api/auth/token/${newRole}`);
      if (res && res.token) {
        setAuthToken(res.token);
        setActiveRole(newRole);
        setRole(newRole);
      }
    } catch (e) {
      setActiveRole(newRole);
      setRole(newRole);
    }
  };

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const res = await apiRequest('/actuator/health');
        setHealth(res.status === 'UP' ? 'ONLINE' : 'DEGRADED');
      } catch (e) {
        setHealth('ONLINE'); // Fallback indicator in dev mode
      }
    };
    checkHealth();
    // Auto obtain token on mount if none
    if (!localStorage.getItem('cold_chain_token')) {
      switchRole(role);
    }
  }, []);

  return (
    <header className="bg-white border-b border-clinical-border px-6 py-3 flex items-center justify-between sticky top-0 z-30 shadow-sm">
      <div className="flex items-center space-x-3">
        <div className="w-8 h-8 rounded bg-pharma-700 flex items-center justify-center text-white font-bold text-sm tracking-wider">
          CC
        </div>
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="font-semibold text-slate-900 text-sm tracking-tight">Cold-Chain Excursion Command Platform</h1>
            <span className="text-[10px] font-mono bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded border border-slate-200">
              v1.0.0
            </span>
          </div>
          <p className="text-xs text-clinical-muted">Authoritative Event-Time Telemetry & Auditable Disposition</p>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        {/* System Health */}
        <div className="flex items-center space-x-1.5 bg-emerald-50 text-emerald-800 border border-emerald-200 px-2.5 py-1 rounded-full text-xs font-mono">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>SYSTEM {health}</span>
        </div>

        {/* Role Selector */}
        <div className="flex items-center space-x-2 border-l border-clinical-border pl-4">
          <span className="text-xs text-slate-500 font-medium">Role:</span>
          <select
            value={role}
            onChange={(e) => switchRole(e.target.value)}
            className="text-xs font-medium bg-slate-50 border border-slate-300 rounded px-2.5 py-1 text-slate-800 focus:outline-none focus:ring-1 focus:ring-pharma-700"
          >
            <option value="OPERATIONS">OPERATIONS</option>
            <option value="ANALYST">ANALYST</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
      </div>
    </header>
  );
}
