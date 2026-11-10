import React, { useEffect, useState } from 'react';
import { FileSearch, UserCheck, ShieldCheck, Plus, AlertCircle, Clock } from 'lucide-react';
import { apiRequest } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import EvidenceComparisonCard from '../components/EvidenceComparisonCard';
import DispositionModal from '../components/DispositionModal';

export default function Investigation() {
  const [investigations, setInvestigations] = useState([]);
  const [selectedInv, setSelectedInv] = useState(null);
  const [decisions, setDecisions] = useState([]);
  const [showInspectionModal, setShowInspectionModal] = useState(false);
  const [showDispositionModal, setShowDispositionModal] = useState(false);

  // Manual inspection form state
  const [inspectionForm, setInspectionForm] = useState({
    inspectorName: 'Dr. Sarah Vance, QA Officer',
    inspectionTime: new Date().toISOString(),
    observedTemperatureC: '6.2',
    packageIntegrityStatus: 'INTACT',
    visualCondition: 'Dry ice container intact; package tamper seals fully unbreached',
    notes: 'Secondary manual thermometer reading at receiving dock'
  });

  const loadData = async () => {
    try {
      const excs = await apiRequest('/api/excursions');
      const invList = [];
      for (const exc of excs) {
        try {
          const inv = await apiRequest(`/api/investigations/excursion/${exc.id}`);
          inv.excursionData = exc;
          invList.push(inv);
        } catch (e) {
          // No investigation yet
        }
      }
      setInvestigations(invList);
      if (invList.length > 0 && !selectedInv) {
        selectInvestigation(invList[0]);
      } else if (selectedInv) {
        const updated = invList.find(i => i.id === selectedInv.id);
        if (updated) selectInvestigation(updated);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const selectInvestigation = async (inv) => {
    setSelectedInv(inv);
    try {
      const decs = await apiRequest(`/api/investigations/${inv.id}/decisions`);
      setDecisions(decs || []);
    } catch (e) {
      setDecisions([]);
    }
  };

  useEffect(() => {
    loadData();
    const listener = () => loadData();
    window.addEventListener('cold_chain_refresh', listener);
    return () => window.removeEventListener('cold_chain_refresh', listener);
  }, []);

  const handleAddInspection = async (e) => {
    e.preventDefault();
    try {
      await apiRequest(`/api/investigations/${selectedInv.id}/manual-inspection`, {
        method: 'POST',
        body: JSON.stringify({
          ...inspectionForm,
          observedTemperatureC: parseFloat(inspectionForm.observedTemperatureC)
        })
      });
      setShowInspectionModal(false);
      loadData();
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900 tracking-tight">Pharmaceutical QA Investigations</h2>
          <p className="text-xs text-clinical-muted">Independent evidence correlation, contradictory evidence review, and auditable lot disposition</p>
        </div>
      </div>

      {investigations.length === 0 ? (
        <div className="bg-white p-8 border border-clinical-border rounded-lg text-center text-slate-400 text-xs">
          No investigations on record. Run Scenario 2, 3, or 6 above to generate an excursion investigation.
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Investigation Queue Sidebar */}
          <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card lg:col-span-1">
            <div className="px-4 py-3 border-b border-clinical-border bg-slate-50 flex items-center justify-between">
              <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">Investigation Queue</span>
              <span className="text-[11px] font-mono text-slate-500">{investigations.length} files</span>
            </div>
            <div className="divide-y divide-slate-100 max-h-[600px] overflow-y-auto">
              {investigations.map((inv) => (
                <div
                  key={inv.id}
                  onClick={() => selectInvestigation(inv)}
                  className={`p-3.5 cursor-pointer transition text-xs ${
                    selectedInv?.id === inv.id ? 'bg-slate-100/90 border-l-4 border-pharma-700' : 'hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono font-bold text-slate-900">{inv.id}</span>
                    <StatusBadge status={inv.status} />
                  </div>
                  <div className="text-[11px] text-slate-600 mt-1">
                    Shipment: <span className="font-mono">{inv.shipmentId}</span>
                  </div>
                  <div className="flex items-center justify-between text-[10px] text-slate-400 mt-2">
                    <span className="flex items-center space-x-1">
                      <Clock className="w-3 h-3" />
                      <span>SLA: {new Date(inv.slaDueAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </span>
                    <span>{inv.manualInspections?.length || 0} manual inspections</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Investigation File Detail */}
          {selectedInv && (
            <div className="lg:col-span-2 space-y-4">
              {/* Header card */}
              <div className="bg-white border border-clinical-border rounded-lg p-4 clinical-card flex items-center justify-between flex-wrap gap-2">
                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="text-sm font-bold text-slate-900 font-mono">{selectedInv.id}</h3>
                    <StatusBadge status={selectedInv.status} />
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Linked Excursion: <span className="font-mono">{selectedInv.excursionId}</span>
                  </p>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setShowInspectionModal(true)}
                    className="inline-flex items-center space-x-1 px-3 py-1.5 bg-slate-50 hover:bg-slate-100 border border-slate-300 text-slate-700 rounded text-xs font-semibold transition"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Manual Inspection</span>
                  </button>

                  {selectedInv.status !== 'COMPLETED' && (
                    <button
                      onClick={() => setShowDispositionModal(true)}
                      className="inline-flex items-center space-x-1 px-3 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded text-xs font-semibold shadow-sm transition"
                    >
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Execute Disposition</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Side-by-side Evidence Comparison Card */}
              <EvidenceComparisonCard
                excursion={selectedInv.excursionData}
                manualInspections={selectedInv.manualInspections || []}
              />

              {/* Historical Decisions Ledger */}
              <div className="bg-white border border-clinical-border rounded-lg p-4 clinical-card">
                <div className="flex items-center justify-between border-b border-clinical-border pb-2.5 mb-3">
                  <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                    Append-Only Disposition Decisions ({decisions.length})
                  </h4>
                  <span className="text-[10px] text-slate-400 font-mono">Immutable Audit Record</span>
                </div>

                {decisions.length === 0 ? (
                  <div className="text-slate-400 text-xs py-4 text-center">
                    No disposition decision recorded yet. File remains in pending/active status.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {decisions.map((d) => (
                      <div key={d.id} className="p-3 bg-slate-50 border border-slate-200 rounded text-xs space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-900 flex items-center space-x-2">
                            <span>Decision:</span>
                            <StatusBadge status={d.decision} />
                          </span>
                          <span className="font-mono text-[11px] text-slate-500">
                            {new Date(d.decidedAt).toLocaleString()}
                          </span>
                        </div>
                        <div className="text-slate-600">
                          Investigator: <strong className="text-slate-800">{d.investigatorName}</strong> • Rule Set: <span className="font-mono">{d.ruleVersion}</span>
                        </div>
                        <div className="p-2 bg-white border border-slate-200 rounded text-slate-700 italic text-[11px]">
                          "{d.reason}"
                        </div>
                        <div className="text-[10px] text-slate-500 font-mono">
                          Affected Lots: {d.affectedLotIds?.join(', ') || 'N/A'}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Manual Inspection Modal */}
      {showInspectionModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-lg border border-clinical-border shadow-xl max-w-md w-full p-5 text-xs">
            <h3 className="text-sm font-bold text-slate-900 mb-3">Record Physical Receiving Dock Inspection</h3>
            <form onSubmit={handleAddInspection} className="space-y-3">
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Inspector Full Name & Title</label>
                <input
                  type="text"
                  value={inspectionForm.inspectorName}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, inspectorName: e.target.value })}
                  className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Measured Temperature (°C)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={inspectionForm.observedTemperatureC}
                    onChange={(e) => setInspectionForm({ ...inspectionForm, observedTemperatureC: e.target.value })}
                    className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none font-mono"
                    required
                  />
                </div>
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Package Integrity</label>
                  <select
                    value={inspectionForm.packageIntegrityStatus}
                    onChange={(e) => setInspectionForm({ ...inspectionForm, packageIntegrityStatus: e.target.value })}
                    className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                  >
                    <option value="INTACT">INTACT</option>
                    <option value="DAMAGED">DAMAGED</option>
                    <option value="SEAL_BROKEN">SEAL BROKEN</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Visual Observations</label>
                <input
                  type="text"
                  value={inspectionForm.visualCondition}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, visualCondition: e.target.value })}
                  className="w-full border border-slate-300 rounded px-2.5 py-1.5 focus:ring-1 focus:ring-pharma-700 outline-none"
                />
              </div>
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Inspection Notes</label>
                <textarea
                  rows={2}
                  value={inspectionForm.notes}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, notes: e.target.value })}
                  className="w-full border border-slate-300 rounded p-2 focus:ring-1 focus:ring-pharma-700 outline-none"
                />
              </div>
              <div className="flex justify-end space-x-2 pt-2 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowInspectionModal(false)}
                  className="px-3 py-1.5 border border-slate-300 rounded text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded font-medium"
                >
                  Preserve Inspection Evidence
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Disposition Modal */}
      {showDispositionModal && (
        <DispositionModal
          investigationId={selectedInv.id}
          ruleVersion={selectedInv.excursionData?.ruleVersion || 'v1.0.0'}
          lotIds={selectedInv.excursionData?.affectedLotIds || []}
          onClose={() => setShowDispositionModal(false)}
          onSuccess={() => {
            setShowDispositionModal(false);
            loadData();
          }}
        />
      )}
    </div>
  );
}
