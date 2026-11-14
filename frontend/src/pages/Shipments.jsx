import React, { useCallback, useEffect, useState } from 'react';
import { ArrowRight, Package, Plus, Search } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiRequest, getActiveRole } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import Modal from '../components/Modal';
import { Archive, Trash2 } from 'lucide-react';

const readable = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());

export default function Shipments() {
  const [searchParams] = useSearchParams();
  const attentionOnly = searchParams.get('filter') === 'attention';
  const role = getActiveRole();
  const [shipments, setShipments] = useState([]);
  const [query, setQuery] = useState('');
  const [visibility, setVisibility] = useState('ACTIVE');
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const emptyForm = () => ({ shipmentReference: '', trackingNumber: '', origin: '', destination: '', storageProfileId: 'PROFILE-REF', status: 'PLANNED', expectedDeliveryAt: '' });
  const [form, setForm] = useState(emptyForm);

  const load = useCallback(async () => {
    setError('');
    try { setShipments(await apiRequest(`/api/shipments${visibility === 'ARCHIVED' ? '?includeArchived=true' : ''}`) || []); }
    catch (e) { setError(e.message || 'Could not load shipments.'); }
    finally { setLoading(false); }
  }, [visibility]);
  useEffect(() => { load(); window.addEventListener('cold_chain_refresh', load); return () => window.removeEventListener('cold_chain_refresh', load); }, [load]);

  const filtered = shipments.filter(s => (!attentionOnly || ['TEMPERATURE_ISSUE', 'AT_RISK'].includes(s.condition))
    && [s.shipmentId, s.id, s.trackingNumber, s.origin, s.destination].some(v => String(v || '').toLowerCase().includes(query.toLowerCase())));
  const submit = async e => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      await apiRequest('/api/shipments', { method: 'POST', body: JSON.stringify({ ...form, expectedDeliveryAt: form.expectedDeliveryAt ? new Date(form.expectedDeliveryAt).toISOString() : null, expectedCadenceMinutes: 5, allowedLatenessMinutes: 120 }) });
      setShowForm(false); setMessage('Shipment added.'); setForm(emptyForm()); await load();
      setTimeout(() => setMessage(''), 3000);
    } catch (e2) { setError(e2.message || 'Unable to register this shipment.'); }
    finally { setSaving(false); }
  };
  const removeOrArchive = async shipment => {
    const archive = shipment.hasOperationalHistory;
    const reason = archive ? '' : window.prompt(`Why should ${shipment.shipmentId} be removed from active operations?`);
    if (!archive && (!reason || !reason.trim())) return;
    const message = archive
      ? `${shipment.shipmentId} has operational history. Archive it while retaining its records?`
      : `Remove ${shipment.shipmentId} from active operations? Its record and audit history will be retained.`;
    if (!window.confirm(message)) return;
    try {
      if (archive) await apiRequest(`/api/shipments/${shipment.id}/archive`, { method: 'POST' });
      else await apiRequest(`/api/shipments/${shipment.id}`, { method: 'DELETE', body: JSON.stringify({ reason: reason.trim() }) });
      setMessage(archive ? 'Shipment archived.' : 'Shipment removed from active operations.');
      await load();
    } catch (e) { setError(e.message || 'Unable to update shipment lifecycle.'); }
  };

  return (
    <div className="page-stack">
      <div className="page-heading-row">
        <div><span className="eyebrow">COLD CHAIN</span><h1>Shipments</h1><p>Track route, condition and lot readiness in one place.</p></div>
        {role === 'OPERATIONS' && <button className="btn-primary" onClick={() => { setForm(emptyForm()); setShowForm(true); }}><Plus size={16} /> Register shipment</button>}
      </div>
      {message && <div className="success-message">{message}</div>}
      {error && <div className="alert-card error">{error}</div>}
      <section className="content-card">
        <div className="list-toolbar">
          <div className="list-count"><strong>{attentionOnly ? filtered.length : shipments.length}</strong> {attentionOnly ? 'shipments need attention' : shipments.length === 1 ? 'shipment' : 'shipments'}</div>
          <div className="list-toolbar-controls"><select className="form-select" value={visibility} onChange={e => setVisibility(e.target.value)} aria-label="Shipment visibility"><option value="ACTIVE">Active shipments</option><option value="ARCHIVED">Archived shipments</option></select><label className="search-box"><Search size={16} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search shipments" aria-label="Search shipments" /></label></div>
        </div>
        {loading ? <div className="loading-card">Loading shipments…</div> : filtered.length === 0 ? (
            <div className="empty-state"><span className="empty-icon"><Package size={20} /></span><strong>{query ? 'No matching shipments' : attentionOnly ? 'No shipments need attention' : visibility === 'ARCHIVED' ? 'No archived shipments' : 'No shipments registered'}</strong><span>{query ? 'Try another shipment, tracking number or location.' : attentionOnly ? 'Shipments with a temperature issue or at-risk condition will appear here.' : visibility === 'ARCHIVED' ? 'Archived shipments remain available for traceability.' : 'New shipments will appear here after registration.'}</span></div>
        ) : (
          <div className="table-scroll"><table className="business-table"><thead><tr><th>Shipment</th><th>Route</th><th>Storage</th><th>Condition</th><th>Status</th><th /></tr></thead>
            <tbody>{filtered.map(s => <tr key={s.id}>
              <td><Link className="primary-cell" to={`/shipments/${s.id}`}>{s.shipmentId || s.id}</Link><span className="secondary-cell">{s.trackingNumber}</span></td>
              <td>{s.origin} <span className="route-arrow">→</span> {s.destination}</td>
              <td>{s.storageType || s.storageProfileName || '—'}</td>
              <td><Condition condition={s.condition} /></td>
              <td><StatusBadge status={s.status} /></td>
              <td className="shipment-row-actions"><Link className="icon-link" aria-label={`Open ${s.shipmentId || s.id}`} to={`/shipments/${s.id}`}><ArrowRight size={17} /></Link>{role === 'OPERATIONS' && !s.archivedAt && !s.deletedAt && <button className="icon-link lifecycle-action" onClick={() => removeOrArchive(s)} aria-label={s.hasOperationalHistory ? `Archive ${s.shipmentId}` : `Remove ${s.shipmentId}`} title={s.hasOperationalHistory ? 'Archive shipment' : 'Remove shipment'}>{s.hasOperationalHistory ? <Archive size={16}/> : <Trash2 size={16}/>}</button>}</td>
            </tr>)}</tbody></table></div>
        )}
      </section>

      {showForm && <Modal title="Register shipment" eyebrow="OPERATIONS" labelledBy="register-title" onClose={() => setShowForm(false)}>
        <form onSubmit={submit}>
          <div className="form-stack">
            <div className="form-grid">
              <Field label="Shipment reference"><input className="form-input" required maxLength="128" placeholder="e.g. Dublin to St James’s · Batch 14" value={form.shipmentReference} onChange={e => setForm({ ...form, shipmentReference: e.target.value })} /></Field>
              <Field label="Tracking number"><input className="form-input" required maxLength="128" placeholder="Carrier tracking reference" value={form.trackingNumber} onChange={e => setForm({ ...form, trackingNumber: e.target.value })} /></Field>
              <Field label="Origin"><input className="form-input" required placeholder="City or facility" value={form.origin} onChange={e => setForm({ ...form, origin: e.target.value })} /></Field>
              <Field label="Destination"><input className="form-input" required placeholder="City or facility" value={form.destination} onChange={e => setForm({ ...form, destination: e.target.value })} /></Field>
              <Field label="Initial status"><select className="form-select full-width" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}><option value="PLANNED">Planned</option><option value="IN_TRANSIT">In transit</option></select></Field>
              <Field label="Expected delivery"><input className="form-input" type="datetime-local" value={form.expectedDeliveryAt} onChange={e => setForm({ ...form, expectedDeliveryAt: e.target.value })} /></Field>
            </div>
            <Field label="Storage condition"><select className="form-select full-width" value={form.storageProfileId} onChange={e => setForm({ ...form, storageProfileId: e.target.value })}><option value="PROFILE-REF">Refrigerated · 2–8°C</option><option value="PROFILE-COOL">Cool · 8–15°C</option><option value="PROFILE-CRT">Controlled room · 15–25°C</option><option value="PROFILE-FRZ">Frozen · −25 to −10°C</option></select></Field>
            {error && <div className="inline-error" role="alert">{error}</div>}
          </div>
          <div className="modal-footer"><button type="button" className="btn-secondary" onClick={() => setShowForm(false)}>Cancel</button><button className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Register shipment'}</button></div>
        </form>
      </Modal>}
    </div>
  );
}

function Field({ label, children }) { return <label className="form-field"><span>{label}</span>{children}</label>; }
function Condition({ condition }) { const c = String(condition || 'UNKNOWN').toUpperCase(); const tone = c === 'NORMAL' ? 'normal' : c === 'UNKNOWN' ? 'unknown' : c === 'AT_RISK' ? 'at-risk' : 'alert'; return <span className={`condition-label ${tone}`}><i />{readable(c)}</span>; }
