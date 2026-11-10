import React, { useEffect, useState } from 'react';
import { History, Filter, Shield } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';

export default function AuditHistory() {
  const [audits, setAudits] = useState([]);
  const [filterType, setFilterType] = useState('ALL');

  const loadAudits = async () => {
    try {
      const data = await apiRequest('/api/audit?page=0&size=100');
      setAudits(data.content || []);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadAudits();
    const listener = () => loadAudits();
    window.addEventListener('cold_chain_refresh', listener);
    return () => window.removeEventListener('cold_chain_refresh', listener);
  }, []);

  const filtered = filterType === 'ALL'
    ? audits
    : audits.filter(a => a.entityType === filterType);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">Immutable Compliance Audit Ledger</h2>
          <p className="text-xs text-clinical-muted">Append-only chronological audit log preserving every platform state mutation, actor, and rationale</p>
        </div>

        <div className="flex items-center space-x-2">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="text-xs border border-slate-300 rounded px-2.5 py-1 bg-white text-slate-700 outline-none"
          >
            <option value="ALL">All Entity Types</option>
            <option value="SHIPMENT">Shipments</option>
            <option value="LOT">Lots</option>
            <option value="SENSOR_EVENT">Sensor Events</option>
            <option value="EXCURSION">Excursions</option>
            <option value="INVESTIGATION">Investigations</option>
            <option value="DECISION">Decisions</option>
          </select>
        </div>
      </div>

      <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card">
        <div className="px-5 py-3 border-b border-clinical-border bg-slate-50 flex items-center justify-between">
          <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">Audit Records ({filtered.length})</span>
          <span className="text-[11px] font-mono text-slate-400">Read-Only Compliance View</span>
        </div>

        {filtered.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs">
            No audit records matching selected filter.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] text-slate-500 font-semibold uppercase tracking-wider font-mono">
                  <th className="py-2.5 px-4">Timestamp (UTC)</th>
                  <th className="py-2.5 px-4">Entity</th>
                  <th className="py-2.5 px-4">Entity ID</th>
                  <th className="py-2.5 px-4">Action</th>
                  <th className="py-2.5 px-4">Actor</th>
                  <th className="py-2.5 px-4">Audit Rationale</th>
                  <th className="py-2.5 px-4">State Mutation</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-[11px]">
                {filtered.map((record) => (
                  <tr key={record.id} className="hover:bg-slate-50 transition">
                    <td className="py-2.5 px-4 font-mono text-slate-500 whitespace-nowrap">
                      {new Date(record.timestamp).toISOString()}
                    </td>
                    <td className="py-2.5 px-4 font-mono font-semibold text-slate-700">
                      {record.entityType}
                    </td>
                    <td className="py-2.5 px-4 font-mono text-slate-900">{record.entityId}</td>
                    <td className="py-2.5 px-4">
                      <StatusBadge status={record.action} />
                    </td>
                    <td className="py-2.5 px-4 font-medium text-slate-800">{record.actor}</td>
                    <td className="py-2.5 px-4 text-slate-600 max-w-xs">{record.rationale || '-'}</td>
                    <td className="py-2.5 px-4 font-mono text-[10px] text-slate-500 max-w-sm truncate" title={record.payloadAfter}>
                      {record.payloadAfter || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
