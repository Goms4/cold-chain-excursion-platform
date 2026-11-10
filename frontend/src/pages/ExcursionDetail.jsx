import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, ArrowUpRight, Box, Shield, FileSearch } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import SeverityPill from '../components/SeverityPill';

export default function ExcursionDetail() {
  const { id } = useParams();
  const [excursion, setExcursion] = useState(null);

  const loadExcursion = async () => {
    try {
      const data = await apiRequest(`/api/excursions/${id}`);
      setExcursion(data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadExcursion();
  }, [id]);

  if (!excursion) {
    return <div className="text-xs text-slate-500 p-8">Loading excursion details...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-3">
        <Link to="/excursions" className="p-1.5 bg-white border border-slate-200 rounded hover:bg-slate-50 text-slate-600">
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-lg font-bold text-slate-900 tracking-tight font-mono">{excursion.id}</h2>
            <SeverityPill severity={excursion.severity} />
            <StatusBadge status={excursion.status} />
          </div>
          <p className="text-xs text-clinical-muted">Shipment: {excursion.shipmentId} • Rule Version: {excursion.ruleVersion}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Peak Temperature</span>
          <div className="text-xl font-bold text-rose-700 mt-1 font-mono">{excursion.peakTemperatureC}°C</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Worst-case observed breach</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Excursion Duration</span>
          <div className="text-xl font-bold text-slate-900 mt-1 font-mono">{excursion.durationMinutes} mins</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Cumulative event-time breach</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Start Event-Time</span>
          <div className="text-sm font-mono text-slate-800 mt-1">
            {new Date(excursion.startEventTime).toLocaleTimeString()}
          </div>
          <div className="text-[10px] text-slate-400 mt-0.5">{new Date(excursion.startEventTime).toLocaleDateString()}</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card flex flex-col justify-between">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Formal Investigation</span>
          <Link
            to="/investigations"
            className="mt-2 inline-flex items-center justify-center space-x-1 px-3 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded text-xs font-semibold shadow-sm transition"
          >
            <FileSearch className="w-3.5 h-3.5" />
            <span>Open Investigation File</span>
          </Link>
        </div>
      </div>

      {/* Affected Lots */}
      <div className="bg-white border border-clinical-border rounded-lg p-5 clinical-card">
        <div className="flex items-center space-x-2 border-b border-clinical-border pb-3 mb-4">
          <Box className="w-4 h-4 text-pharma-700" />
          <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
            Traceable Affected Pharmaceutical Lots ({excursion.affectedLotIds?.length || 0})
          </h3>
        </div>

        {(!excursion.affectedLotIds || excursion.affectedLotIds.length === 0) ? (
          <div className="text-slate-400 text-xs py-4 text-center">No lots linked to this incident.</div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {excursion.affectedLotIds.map(lotId => (
              <div key={lotId} className="p-3 bg-slate-50 border border-slate-200 rounded text-xs font-mono">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-900">{lotId}</span>
                  <StatusBadge status="HIGH_RISK" />
                </div>
                <div className="text-[11px] text-slate-500 mt-1 font-sans">
                  Subject to quarantine pending investigator disposition.
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
