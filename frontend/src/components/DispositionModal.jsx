import React, { useState } from 'react';
import { X, CheckCircle, AlertTriangle, XCircle, ShieldCheck } from 'lucide-react';
import { apiRequest } from '../api/client';

export default function DispositionModal({ investigationId, ruleVersion = 'v1.0.0', lotIds = [], onClose, onSuccess }) {
  const [decision, setDecision] = useState('RELEASE');
  const [investigatorName, setInvestigatorName] = useState('Dr. Alex Chen, QA Lead');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason || reason.trim().length < 5) {
      setError('A comprehensive disposition rationale is required by regulatory audit standards.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await apiRequest(`/api/investigations/${investigationId}/decision`, {
        method: 'POST',
        body: JSON.stringify({
          decision,
          investigatorName,
          reason,
          ruleVersion,
          affectedLotIds: lotIds.length > 0 ? lotIds : ['LOT-DEFAULT']
        })
      });
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.message || 'Failed to record disposition decision.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-lg border border-clinical-border shadow-xl max-w-lg w-full overflow-hidden">
        <div className="px-5 py-3.5 border-b border-clinical-border flex items-center justify-between bg-slate-50">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
              Execute Disposition Decision (Append-Only)
            </h3>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-600">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
          {error && (
            <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-800 rounded font-medium">
              {error}
            </div>
          )}

          <div>
            <label className="block text-slate-600 font-semibold mb-1.5 uppercase tracking-wider text-[11px]">
              Disposition Action
            </label>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setDecision('RELEASE')}
                className={`py-2 px-3 rounded border text-xs font-semibold flex items-center justify-center space-x-1.5 transition ${
                  decision === 'RELEASE'
                    ? 'bg-emerald-50 text-emerald-800 border-emerald-500 ring-1 ring-emerald-500'
                    : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                <CheckCircle className="w-3.5 h-3.5" />
                <span>RELEASE</span>
              </button>
              <button
                type="button"
                onClick={() => setDecision('HOLD')}
                className={`py-2 px-3 rounded border text-xs font-semibold flex items-center justify-center space-x-1.5 transition ${
                  decision === 'HOLD'
                    ? 'bg-amber-50 text-amber-800 border-amber-500 ring-1 ring-amber-500'
                    : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>HOLD</span>
              </button>
              <button
                type="button"
                onClick={() => setDecision('REJECT')}
                className={`py-2 px-3 rounded border text-xs font-semibold flex items-center justify-center space-x-1.5 transition ${
                  decision === 'REJECT'
                    ? 'bg-rose-50 text-rose-800 border-rose-500 ring-1 ring-rose-500'
                    : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                <XCircle className="w-3.5 h-3.5" />
                <span>REJECT</span>
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-600 font-semibold mb-1">Investigator Name</label>
              <input
                type="text"
                value={investigatorName}
                onChange={(e) => setInvestigatorName(e.target.value)}
                className="w-full border border-slate-300 rounded px-2.5 py-1.5 text-xs focus:ring-1 focus:ring-pharma-700 outline-none"
                required
              />
            </div>
            <div>
              <label className="block text-slate-600 font-semibold mb-1">Rule Version</label>
              <input
                type="text"
                value={ruleVersion}
                readOnly
                className="w-full bg-slate-50 border border-slate-200 rounded px-2.5 py-1.5 text-xs text-slate-600 font-mono outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-600 font-semibold mb-1">Affected Lot IDs ({lotIds.length})</label>
            <div className="bg-slate-50 border border-slate-200 rounded p-2 text-[11px] font-mono text-slate-700 max-h-16 overflow-y-auto">
              {lotIds.length > 0 ? lotIds.join(', ') : 'None assigned'}
            </div>
          </div>

          <div>
            <label className="block text-slate-600 font-semibold mb-1">
              Mandatory Disposition Rationale <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={4}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Cite stability data, mean kinetic temperature, container inspection, or manufacturer release criteria..."
              className="w-full border border-slate-300 rounded p-2 text-xs focus:ring-1 focus:ring-pharma-700 outline-none"
              required
            />
            <p className="text-[10px] text-slate-400 mt-1">
              This record is written directly to the immutable append-only ledger and cannot be deleted.
            </p>
          </div>

          <div className="flex justify-end space-x-2 pt-2 border-t border-clinical-border">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-1.5 bg-pharma-700 hover:bg-pharma-800 text-white rounded font-medium shadow-sm transition"
            >
              {submitting ? 'Committing...' : 'Commit Immutable Disposition'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
