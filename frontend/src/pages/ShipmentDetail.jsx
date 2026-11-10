import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Box, Shield, Cpu, Clock, AlertTriangle, Layers } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import TemperatureTimeline from '../components/TemperatureTimeline';

export default function ShipmentDetail() {
  const { id } = useParams();
  const [shipment, setShipment] = useState(null);
  const [lots, setLots] = useState([]);
  const [sensors, setSensors] = useState([]);
  const [events, setEvents] = useState([]);
  const [rawEvents, setRawEvents] = useState([]);
  const [viewTab, setViewTab] = useState('events');

  const loadData = async () => {
    try {
      const [shp, lotList, sensorList, eventList, rawList] = await Promise.all([
        apiRequest(`/api/shipments/${id}`),
        apiRequest(`/api/shipments/${id}/lots`),
        apiRequest(`/api/shipments/${id}/sensors`),
        apiRequest(`/api/shipments/${id}/sensor-events`),
        apiRequest(`/api/shipments/${id}/raw-events`)
      ]);
      setShipment(shp);
      setLots(lotList);
      setSensors(sensorList);
      setEvents(eventList);
      setRawEvents(rawList);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadData();
    const listener = () => loadData();
    window.addEventListener('cold_chain_refresh', listener);
    return () => window.removeEventListener('cold_chain_refresh', listener);
  }, [id]);

  if (!shipment) {
    return <div className="text-xs text-slate-500 p-8">Loading shipment telemetry...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-3">
        <Link to="/shipments" className="p-1.5 bg-white border border-slate-200 rounded hover:bg-slate-50 text-slate-600">
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-lg font-bold text-slate-900 tracking-tight font-mono">{shipment.id}</h2>
            <StatusBadge status={shipment.status} />
          </div>
          <p className="text-xs text-clinical-muted">{shipment.origin} &rarr; {shipment.destination} • Tracking #{shipment.trackingNumber}</p>
        </div>
      </div>

      {/* Shipment Specs Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Applicable Storage Profile</span>
          <div className="font-semibold text-slate-800 mt-1">{shipment.storageProfileName || shipment.storageProfileId}</div>
          <div className="text-[10px] text-slate-400 font-mono mt-0.5">Authoritative criteria</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Telemetry Cadence</span>
          <div className="font-semibold text-slate-800 mt-1 font-mono">{shipment.expectedCadenceMinutes} minutes</div>
          <div className="text-[10px] text-purple-600 font-mono mt-0.5">Gaps flagged UNKNOWN</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Lateness Watermark</span>
          <div className="font-semibold text-slate-800 mt-1 font-mono">{shipment.allowedLatenessMinutes} minutes</div>
          <div className="text-[10px] text-rose-600 font-mono mt-0.5">Late arrival quarantine</div>
        </div>

        <div className="bg-white p-3.5 border border-clinical-border rounded-lg clinical-card">
          <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Assigned Lots</span>
          <div className="font-semibold text-slate-800 mt-1">{lots.length} Pharmaceutical Batches</div>
          <div className="text-[10px] text-slate-400 mt-0.5">Traceable risk cascade</div>
        </div>
      </div>

      {/* Temperature Timeline Visual */}
      <TemperatureTimeline events={events} profile={{ targetMinC: 15, targetMaxC: 25, minorMaxC: 30, criticalMaxC: 40 }} />

      {/* Associated Lots & Sensors */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Lots Table */}
        <div className="bg-white border border-clinical-border rounded-lg p-4 clinical-card">
          <div className="flex items-center space-x-2 border-b border-clinical-border pb-2.5 mb-3">
            <Box className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Lots Carried ({lots.length})</h3>
          </div>
          {lots.length === 0 ? (
            <div className="text-slate-400 text-xs py-4 text-center">No lots registered on this consignment.</div>
          ) : (
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500 text-[10px] uppercase font-semibold">
                  <th className="py-1.5 text-left">Lot ID</th>
                  <th className="py-1.5 text-left">Product</th>
                  <th className="py-1.5 text-left">Qty</th>
                  <th className="py-1.5 text-left">Risk Level</th>
                  <th className="py-1.5 text-right">Disposition</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {lots.map(l => (
                  <tr key={l.id}>
                    <td className="py-2 font-mono font-medium text-slate-900">{l.id}</td>
                    <td className="py-2 text-slate-700">{l.productName}</td>
                    <td className="py-2 font-mono text-slate-600">{l.quantity}</td>
                    <td className="py-2">
                      <StatusBadge status={l.riskLevel} />
                    </td>
                    <td className="py-2 text-right">
                      <StatusBadge status={l.dispositionStatus} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Assigned Sensors */}
        <div className="bg-white border border-clinical-border rounded-lg p-4 clinical-card">
          <div className="flex items-center space-x-2 border-b border-clinical-border pb-2.5 mb-3">
            <Cpu className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Assigned Sensor Probes ({sensors.length})</h3>
          </div>
          {sensors.length === 0 ? (
            <div className="text-slate-400 text-xs py-4 text-center">No sensor assigned.</div>
          ) : (
            <table className="w-full text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500 text-[10px] uppercase font-semibold">
                  <th className="py-1.5 text-left">Sensor ID</th>
                  <th className="py-1.5 text-left">Hardware Model</th>
                  <th className="py-1.5 text-left">Battery</th>
                  <th className="py-1.5 text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {sensors.map(s => (
                  <tr key={s.id}>
                    <td className="py-2 font-mono font-medium text-slate-900">{s.id}</td>
                    <td className="py-2 text-slate-700">{s.model}</td>
                    <td className="py-2 font-mono text-slate-600">{s.batteryLevel}%</td>
                    <td className="py-2 text-right">
                      <StatusBadge status={s.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Telemetry Log Table */}
      <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card">
        <div className="px-5 py-3 border-b border-clinical-border flex items-center justify-between bg-slate-50">
          <div className="flex items-center space-x-3">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">Telemetry Telemetry Audit Ledger</h3>
            <div className="flex space-x-1 border border-slate-300 rounded p-0.5 bg-white text-[11px]">
              <button
                onClick={() => setViewTab('events')}
                className={`px-2.5 py-0.5 rounded font-medium ${viewTab === 'events' ? 'bg-slate-800 text-white' : 'text-slate-600'}`}
              >
                Processed Events ({events.length})
              </button>
              <button
                onClick={() => setViewTab('raw')}
                className={`px-2.5 py-0.5 rounded font-medium ${viewTab === 'raw' ? 'bg-slate-800 text-white' : 'text-slate-600'}`}
              >
                Raw Ingested Payloads ({rawEvents.length})
              </button>
            </div>
          </div>
          <span className="text-[11px] font-mono text-slate-500">Authoritative Event-Time Evaluation</span>
        </div>

        {viewTab === 'events' ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] text-slate-500 font-semibold uppercase tracking-wider">
                  <th className="py-2 px-4">Event ID</th>
                  <th className="py-2 px-4">Sensor</th>
                  <th className="py-2 px-4">Seq</th>
                  <th className="py-2 px-4">Event Time (Authoritative)</th>
                  <th className="py-2 px-4">Ingestion Time</th>
                  <th className="py-2 px-4">Temp (°C)</th>
                  <th className="py-2 px-4">Status</th>
                  <th className="py-2 px-4">Quality Flag</th>
                  <th className="py-2 px-4 text-right">Duplicate?</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {events.map((e) => (
                  <tr key={e.id} className="hover:bg-slate-50 transition">
                    <td className="py-2.5 px-4 font-semibold text-slate-900">{e.eventId}</td>
                    <td className="py-2.5 px-4 text-slate-600">{e.sensor?.id || 'S-001'}</td>
                    <td className="py-2.5 px-4 text-slate-500">{e.sequenceNo ?? '-'}</td>
                    <td className="py-2.5 px-4 text-pharma-800 font-medium">
                      {new Date(e.eventTime).toISOString()}
                    </td>
                    <td className="py-2.5 px-4 text-slate-500">
                      {new Date(e.ingestionTime).toISOString()}
                    </td>
                    <td className="py-2.5 px-4 font-bold text-slate-900">{e.temperatureC}°C</td>
                    <td className="py-2.5 px-4 font-sans">
                      <StatusBadge status={e.status} />
                    </td>
                    <td className="py-2.5 px-4 font-sans">
                      <StatusBadge status={e.qualityFlag} />
                    </td>
                    <td className="py-2.5 px-4 text-right">
                      {e.isDuplicate ? (
                        <span className="text-orange-700 font-semibold font-sans bg-orange-50 px-2 py-0.5 rounded border border-orange-200 text-[10px]">
                          DUPLICATE
                        </span>
                      ) : (
                        <span className="text-emerald-700 font-sans">UNIQUE</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-[10px] text-slate-500 font-semibold uppercase tracking-wider">
                  <th className="py-2 px-4">Raw ID</th>
                  <th className="py-2 px-4">Event ID</th>
                  <th className="py-2 px-4">Ingestion Time</th>
                  <th className="py-2 px-4">Processing Status</th>
                  <th className="py-2 px-4">Immutable Payload</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {rawEvents.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 transition">
                    <td className="py-2.5 px-4 text-slate-400">{r.id}</td>
                    <td className="py-2.5 px-4 font-semibold text-slate-800">{r.eventId}</td>
                    <td className="py-2.5 px-4 text-slate-600">{new Date(r.ingestionTime).toISOString()}</td>
                    <td className="py-2.5 px-4 font-sans">
                      <StatusBadge status={r.processingStatus} />
                    </td>
                    <td className="py-2.5 px-4 text-slate-500 max-w-md truncate" title={r.rawPayload}>
                      {r.rawPayload}
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
