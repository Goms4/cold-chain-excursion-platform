import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Truck, Plus, ArrowRight, Shield } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';

export default function Shipments() {
  const [shipments, setShipments] = useState([]);
  const [showCreate, setShowCreate] = useState(false);
  const [formData, setFormData] = useState({
    id: `SHP-${Math.floor(100 + Math.random() * 900)}`,
    trackingNumber: `TRK-${Math.floor(100000 + Math.random() * 900000)}`,
    origin: 'Copenhagen Bio-Logistics Hub',
    destination: 'Stockholm University Hospital',
    storageProfileId: 'PROFILE-REF',
    expectedCadenceMinutes: 5,
    allowedLatenessMinutes: 120
  });

  const loadShipments = async () => {
    try {
      const data = await apiRequest('/api/shipments');
      setShipments(data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    loadShipments();
    const listener = () => loadShipments();
    window.addEventListener('cold_chain_refresh', listener);
    return () => window.removeEventListener('cold_chain_refresh', listener);
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      await apiRequest('/api/shipments', {
        method: 'POST',
        body: JSON.stringify(formData)
      });
      setShowCreate(false);
      loadShipments();
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">Cold-Chain Shipments</h2>
          <p className="text-xs text-clinical-muted">Temperature-controlled distribution consignments and active monitored lanes</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded text-xs font-semibold shadow-sm transition"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Register Shipment</span>
        </button>
      </div>

      <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-[11px] text-slate-500 font-semibold uppercase tracking-wider">
              <th className="py-2.5 px-4">Shipment ID</th>
              <th className="py-2.5 px-4">Tracking Number</th>
              <th className="py-2.5 px-4">Origin & Destination</th>
              <th className="py-2.5 px-4">Storage Profile</th>
              <th className="py-2.5 px-4">Cadence</th>
              <th className="py-2.5 px-4">Lateness Window</th>
              <th className="py-2.5 px-4">Status</th>
              <th className="py-2.5 px-4 text-right">View Detail</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {shipments.map((s) => (
              <tr key={s.id} className="hover:bg-slate-50/80 transition">
                <td className="py-3 px-4 font-mono font-semibold text-slate-900">{s.id}</td>
                <td className="py-3 px-4 font-mono text-slate-600">{s.trackingNumber}</td>
                <td className="py-3 px-4">
                  <div className="flex items-center space-x-1.5 text-slate-700">
                    <span>{s.origin}</span>
                    <ArrowRight className="w-3 h-3 text-slate-400" />
                    <span className="font-medium text-slate-900">{s.destination}</span>
                  </div>
                </td>
                <td className="py-3 px-4">
                  <span className="inline-flex items-center space-x-1 font-mono text-[11px] bg-slate-50 text-slate-700 px-2 py-0.5 rounded border border-slate-200">
                    <Shield className="w-3 h-3 text-pharma-700" />
                    <span>{s.storageProfileName || s.storageProfileId}</span>
                  </span>
                </td>
                <td className="py-3 px-4 font-mono text-slate-600">{s.expectedCadenceMinutes}m</td>
                <td className="py-3 px-4 font-mono text-slate-600">{s.allowedLatenessMinutes}m</td>
                <td className="py-3 px-4">
                  <StatusBadge status={s.status} />
                </td>
                <td className="py-3 px-4 text-right">
                  <Link
                    to={`/shipments/${s.id}`}
                    className="text-xs font-semibold text-pharma-700 hover:text-pharma-900"
                  >
                    Details &rarr;
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Register Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-lg border border-clinical-border shadow-xl max-w-md w-full p-5 text-xs">
            <h3 className="text-sm font-bold text-slate-900 mb-3">Register Temperature-Controlled Shipment</h3>
            <form onSubmit={handleCreate} className="space-y-3">
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Shipment ID</label>
                <input
                  type="text"
                  value={formData.id}
                  onChange={(e) => setFormData({ ...formData, id: e.target.value })}
                  className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                  required
                />
              </div>
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Tracking Number</label>
                <input
                  type="text"
                  value={formData.trackingNumber}
                  onChange={(e) => setFormData({ ...formData, trackingNumber: e.target.value })}
                  className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Origin</label>
                  <input
                    type="text"
                    value={formData.origin}
                    onChange={(e) => setFormData({ ...formData, origin: e.target.value })}
                    className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Destination</label>
                  <input
                    type="text"
                    value={formData.destination}
                    onChange={(e) => setFormData({ ...formData, destination: e.target.value })}
                    className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Storage Profile</label>
                <select
                  value={formData.storageProfileId}
                  onChange={(e) => setFormData({ ...formData, storageProfileId: e.target.value })}
                  className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                >
                  <option value="PROFILE-CRT">Controlled Room / Ambient (15°C - 25°C)</option>
                  <option value="PROFILE-COOL">Cool (8°C - 15°C)</option>
                  <option value="PROFILE-REF">Refrigerated (2°C - 8°C)</option>
                  <option value="PROFILE-FRZ">Frozen (-25°C to -10°C)</option>
                </select>
              </div>
              <div className="flex justify-end space-x-2 pt-2 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowCreate(false)}
                  className="px-3 py-1.5 border border-slate-300 rounded text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded font-medium"
                >
                  Save Shipment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
