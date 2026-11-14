import React, { useState } from 'react';
import { apiRequest } from '../api/client';
import Modal from './Modal';

const choices = [
  { value: 'RELEASE', label: 'Release shipment', detail: 'Product may continue to distribution.' },
  { value: 'HOLD', label: 'Keep on hold', detail: 'Pause distribution pending more review.' },
  { value: 'REJECT', label: 'Reject shipment', detail: 'Product must not be released.' },
];

export default function DispositionModal({ investigationId, lotIds = [], onClose, onSuccess }) {
  const [decision, setDecision] = useState('RELEASE');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = async event => {
    event.preventDefault();
    if (reason.trim().length < 5) { setError('Please explain the decision before saving.'); return; }
    setBusy(true); setError('');
    try {
      await apiRequest(`/api/investigations/${investigationId}/decision`, { method: 'POST', body: JSON.stringify({ decision, reason: reason.trim() }) });
      onSuccess?.();
    } catch (e) { setError(e.status === 403 ? 'Only an Analyst can record the final shipment decision.' : (e.message || 'Unable to save this decision.')); }
    finally { setBusy(false); }
  };

  return <Modal title="Shipment decision" eyebrow="ANALYST REVIEW" onClose={onClose} labelledBy="decision-title">
    <form onSubmit={submit} className="form-stack">
      <p className="modal-intro">Choose the disposition for the affected product. This decision will be recorded with your Analyst account.</p>
      <div className="decision-choice-list">{choices.map(choice=><button type="button" key={choice.value} className={`decision-choice ${decision===choice.value?'selected':''}`} onClick={()=>setDecision(choice.value)} aria-pressed={decision===choice.value}><span className="choice-radio"/><span><strong>{choice.label}</strong><small>{choice.detail}</small></span></button>)}</div>
      {lotIds.length > 0 && <div className="affected-lots"><span className="field-label">Affected lots</span><strong>{lotIds.join(', ')}</strong></div>}
      <label className="form-field"><span>Reason for decision</span><textarea className="form-textarea" rows="4" value={reason} onChange={e=>setReason(e.target.value)} placeholder="Summarize the evidence and rationale for this decision…" required /></label>
      {error && <div className="inline-error" role="alert">{error}</div>}
      <p className="form-hint">The decision is added to the shipment’s audit history.</p>
      <div className="modal-footer"><button type="button" className="btn-secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="btn-primary" disabled={busy}>{busy?'Saving…':'Record decision'}</button></div>
    </form>
  </Modal>;
}
