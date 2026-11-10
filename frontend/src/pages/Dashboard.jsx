import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, ShieldCheck, Box, Clock, Layers, ArrowUpRight, CheckCircle2 } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import SeverityPill from '../components/SeverityPill';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [excursions, setExcursions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [recon, excs] = await Promise.all([
        apiRequest('/api/reconciliation/summary'),
        apiRequest('/api/excursions')
      ]);
      setSummary(recon);
      setExcursions(excs);
    } catch (e) {
      console.error('Failed to load dashboard metrics', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const listener = () => fetchData();
    window.addEventListener('cold_chain_refresh', listener);
    return () => window.removeEventListener('cold_chain_refresh', listener);
  }, []);

  const activeExcursions = excursions.filter(e => e.status !== 'CLOSED' && e.status !== 'DISPOSITIONED');
  const criticalExcursions = excursions.filter(e => e.severity === 'CRITICAL');

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">Operational Command Dashboard</h2>
          <p className="text-xs text-clinical-muted">Real-time cold-chain health, active incident queue, and telemetry integrity</p>
        </div>
        <button
          onClick={fetchData}
          className="text-xs px-3 py-1 bg-white border border-slate-300 rounded hover:bg-slate-50 text-slate-700 transition"
        >
          Refresh State
        </button>
      </div>

      {/* KPI Tiles */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Total Shipments</span>
          <div className="text-xl font-bold text-slate-900 mt-1">{summary?.totalShipments ?? 0}</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Active transport routes</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-amber-700 uppercase tracking-wider">Active Excursions</span>
          <div className="text-xl font-bold text-amber-600 mt-1">{activeExcursions.length}</div>
          <div className="text-[10px] text-amber-600/80 mt-0.5">Requiring review</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-rose-700 uppercase tracking-wider">Critical Breaches</span>
          <div className="text-xl font-bold text-rose-600 mt-1">{criticalExcursions.length}</div>
          <div className="text-[10px] text-rose-600/80 mt-0.5">Immediate intervention</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Affected Lots</span>
          <div className="text-xl font-bold text-slate-900 mt-1">{summary?.totalLots ?? 0}</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Tracked batches</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-purple-700 uppercase tracking-wider">Unknown Telemetry %</span>
          <div className="text-xl font-bold text-purple-700 mt-1 font-mono">
            {summary && summary.totalRawEvents > 0
              ? ((summary.totalMissingIntervals / (summary.totalRawEvents + summary.totalMissingIntervals)) * 100).toFixed(1) + '%'
              : '0.0%'}
          </div>
          <div className="text-[10px] text-purple-600/80 mt-0.5">Cadence gaps (never safe)</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Audited Records</span>
          <div className="text-xl font-bold text-slate-900 mt-1 font-mono">{summary?.totalAuditRecords ?? 0}</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Immutable ledger</div>
        </div>
      </div>

      {/* Active Excursion Incidents Queue */}
      <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card">
        <div className="px-5 py-3.5 border-b border-clinical-border flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-amber-600" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Active Temperature Excursions & Investigation Queue
            </h3>
          </div>
          <span className="text-xs font-mono text-slate-500">{activeExcursions.length} open incidents</span>
        </div>

        {activeExcursions.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2 opacity-80" />
            No active temperature excursions. All shipments within compliant parameters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[11px] text-slate-500 font-semibold uppercase tracking-wider">
                  <th className="py-2.5 px-4">Incident ID</th>
                  <th className="py-2.5 px-4">Shipment</th>
                  <th className="py-2.5 px-4">Severity</th>
                  <th className="py-2.5 px-4">Peak Temp</th>
                  <th className="py-2.5 px-4">Duration</th>
                  <th className="py-2.5 px-4">Rule Set</th>
                  <th className="py-2.5 px-4">Status</th>
                  <th className="py-2.5 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {activeExcursions.map((exc) => (
                  <tr key={exc.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-mono font-semibold text-slate-800">{exc.id}</td>
                    <td className="py-3 px-4">
                      <Link to={`/shipments/${exc.shipmentId}`} className="text-pharma-700 hover:underline font-medium">
                        {exc.shipmentId}
                      </Link>
                    </td>
                    <td className="py-3 px-4">
                      <SeverityPill severity={exc.severity} />
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-rose-700">{exc.peakTemperatureC}°C</td>
                    <td className="py-3 px-4 font-mono text-slate-600">{exc.durationMinutes} mins</td>
                    <td className="py-3 px-4 font-mono text-slate-500">{exc.ruleVersion}</td>
                    <td className="py-3 px-4">
                      <StatusBadge status={exc.status} />
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Link
                        to={`/investigations`}
                        className="inline-flex items-center space-x-1 text-xs font-semibold text-pharma-700 hover:text-pharma-900 bg-slate-50 px-2.5 py-1 rounded border border-slate-200"
                      >
                        <span>Investigate</span>
                        <ArrowUpRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Data Pipeline Reconciliation Verification */}
      <div className="bg-white border border-clinical-border rounded-lg p-5 clinical-card">
        <div className="flex items-center justify-between border-b border-clinical-border pb-3 mb-3">
          <div className="flex items-center space-x-2">
            <Layers className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Data Pipeline & Event-Time Reconciliation Proof
            </h3>
          </div>
          <span className="text-xs font-mono text-emerald-700 font-semibold bg-emerald-50 px-2.5 py-0.5 rounded border border-emerald-200">
            {summary?.isReconciled ? '100% RECONCILED' : 'DISCREPANCY DETECTED'}
          </span>
        </div>

        <p className="text-xs text-slate-600 mb-4">{summary?.explanation}</p>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs font-mono">
          <div className="p-2.5 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-500 uppercase">Raw Events Logged</span>
            <div className="text-base font-bold text-slate-800">{summary?.totalRawEvents ?? 0}</div>
          </div>
          <div className="p-2.5 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-500 uppercase">Duplicate Suppressed</span>
            <div className="text-base font-bold text-indigo-700">{summary?.totalDuplicateEvents ?? 0}</div>
          </div>
          <div className="p-2.5 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-500 uppercase">Quarantined Late Events</span>
            <div className="text-base font-bold text-rose-700">{summary?.totalQuarantinedEvents ?? 0}</div>
          </div>
          <div className="p-2.5 bg-slate-50 border border-slate-200 rounded">
            <span className="text-[10px] text-slate-500 uppercase">Missing Cadence UNKNOWN</span>
            <div className="text-base font-bold text-purple-700">{summary?.totalMissingIntervals ?? 0}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
