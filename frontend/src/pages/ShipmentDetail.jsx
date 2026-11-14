import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Archive, ArrowLeft, ArrowRight, ClipboardList, Plus, Thermometer, Trash2 } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiRequest, getActiveRole } from '../api/client';
import TemperatureTimeline from '../components/TemperatureTimeline';
import EntityActivityTimeline from '../components/EntityActivityTimeline';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';

const human = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';
const toLocalDateTime = value => { const date = new Date(value); return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16); };
const emptyLotForm = () => ({ productName: '', batchNumber: '', quantity: '', expiryDate: '' });

export default function ShipmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const role = getActiveRole();
  const isOperations = role === 'OPERATIONS';
  const [shipment, setShipment] = useState(null);
  const [lots, setLots] = useState([]);
  const [events, setEvents] = useState([]);
  const [sensors, setSensors] = useState([]);
  const [missing, setMissing] = useState([]);
  const [issues, setIssues] = useState([]);
  const [investigationsByIssue, setInvestigationsByIssue] = useState({});
  const [loading, setLoading] = useState(true);
  const [readingOpen, setReadingOpen] = useState(false);
  const [lotOpen, setLotOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState({ shipmentReference: '', origin: '', destination: '', storageProfileId: '', expectedDeliveryAt: '' });
  const [temperature, setTemperature] = useState('');
  const [eventTime, setEventTime] = useState(() => toLocalDateTime(Date.now()));
  const [selectedLotId, setSelectedLotId] = useState('');
  const [selectedSensorId, setSelectedSensorId] = useState('');
  const [lotForm, setLotForm] = useState(emptyLotForm);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [s, l, e, m, x, sensorData] = await Promise.all([
        apiRequest(`/api/shipments/${id}`), apiRequest(`/api/shipments/${id}/lots?includeArchived=true`),
        apiRequest(`/api/shipments/${id}/sensor-events`),
        apiRequest(`/api/shipments/${id}/missing-intervals`).catch(() => []),
        apiRequest(`/api/shipments/${id}/excursions`).catch(() => []),
        apiRequest(`/api/shipments/${id}/sensors`).catch(() => []),
      ]);
      setShipment(s); setLots(l || []); setEvents(e || []); setMissing(m || []); setIssues(x || []); setSensors(sensorData || []);
      const investigations = await Promise.all((x || []).map(async issue => [
        issue.id,
        await apiRequest(`/api/investigations/excursion/${issue.id}`).catch(() => null),
      ]));
      setInvestigationsByIssue(Object.fromEntries(investigations.filter(([, investigation]) => investigation)));
      const activeSensorRows = (sensorData || []).filter(sensor => sensor.status === 'ACTIVE');
      setSelectedSensorId(current => activeSensorRows.some(sensor => sensor.id === current) ? current : activeSensorRows[0]?.id || '');
      const availableLots = (l || []).filter(item => !item.archivedAt);
      setSelectedLotId(current => availableLots.some(item => item.id === current) ? current : availableLots[0]?.id || '');
    } catch (e2) { setError(e2.message || 'Unable to load shipment details.'); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); window.addEventListener('cold_chain_refresh', load); return () => window.removeEventListener('cold_chain_refresh', load); }, [load]);

  const latestReading = useMemo(() => [...events].filter(e => e.dataQuality !== 'DUPLICATE').sort((a, b) => new Date(b.eventTime) - new Date(a.eventTime))[0], [events]);
  const currentCondition = shipment?.condition || latestReading?.condition || 'UNKNOWN';
  const activeLots = lots.filter(lot => !lot.archivedAt && !lot.deletedAt);
  const activeSensors = sensors.filter(sensor => sensor.status === 'ACTIVE');

  const recordTemperature = async e => {
    e.preventDefault(); if (!selectedLotId || temperature === '') return;
    setBusy(true); setError(''); setResult(null);
    try {
      const payload = { temperatureC: Number(temperature), eventTime: new Date(eventTime).toISOString() };
      if (activeSensors.length > 1) payload.sensorId = selectedSensorId;
      const reading = await apiRequest(`/api/shipments/${id}/lots/${selectedLotId}/temperature`, { method: 'POST', body: JSON.stringify(payload) });
      setResult(reading); setTemperature(''); setReadingOpen(false); await load(); window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e2) { setError(e2.message || 'Unable to save the temperature reading.'); }
    finally { setBusy(false); }
  };

  const registerLot = async e => {
    e.preventDefault(); setBusy(true); setError('');
    try {
      await apiRequest('/api/lots', { method: 'POST', body: JSON.stringify({ ...lotForm, expiryDate: lotForm.expiryDate || null, shipmentId: id, quantity: Number(lotForm.quantity) }) });
      setLotOpen(false); setLotForm(emptyLotForm()); await load();
    } catch (e2) { setError(e2.message || 'Unable to add this lot.'); }
    finally { setBusy(false); }
  };

  const updateShipment = async e => {
    e.preventDefault(); setBusy(true); setError('');
    try {
      await apiRequest(`/api/shipments/${id}`, { method: 'PATCH', body: JSON.stringify({
        ...editForm,
        expectedDeliveryAt: editForm.expectedDeliveryAt ? new Date(editForm.expectedDeliveryAt).toISOString() : null,
      }) });
      setEditOpen(false); await load(); window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e2) { setError(e2.message || 'Unable to update shipment details.'); }
    finally { setBusy(false); }
  };

  const removeOrArchiveLot = async lot => {
    const archive = lot.hasOperationalHistory;
    const reason = archive ? '' : window.prompt(`Why should ${lot.lotId || lot.id} be removed from active operations?`);
    if (!archive && (!reason || !reason.trim())) return;
    const prompt = archive
      ? `${lot.lotId || lot.id} has operational history. Archive it while retaining its records?`
      : `Remove ${lot.lotId || lot.id} from active operations? Its record and audit history will be retained.`;
    if (!window.confirm(prompt)) return;
    setBusy(true); setError('');
    try {
      if (archive) await apiRequest(`/api/lots/${lot.id}/archive`, { method: 'POST' });
      else await apiRequest(`/api/lots/${lot.id}`, { method: 'DELETE', body: JSON.stringify({ reason: reason.trim() }) });
      await load();
    } catch (e) { setError(e.message || 'Unable to update lot lifecycle.'); }
    finally { setBusy(false); }
  };

  if (loading) return <div className="loading-card">Loading shipment…</div>;
  if (!shipment) return <div className="page-stack"><div className="alert-card error">{error || 'Shipment could not be found.'}</div><button className="btn-secondary" onClick={() => navigate('/shipments')}>Back to shipments</button></div>;

  return (
    <div className="page-stack">
      <button className="back-link" onClick={() => navigate('/shipments')}><ArrowLeft size={16} /> Shipments</button>
      <div className="shipment-detail-heading">
        <div><span className="eyebrow">SHIPMENT</span><h1>{shipment.shipmentReference || shipment.shipmentId || shipment.id}</h1><p>{shipment.origin} <span className="route-arrow">→</span> {shipment.destination} · {shipment.trackingNumber}</p></div>
        <div className="detail-heading-status"><Condition condition={currentCondition} /><StatusBadge status={shipment.status} /></div>
        {isOperations && !shipment.archivedAt && !shipment.deletedAt && <button className="btn-secondary" onClick={() => { setEditForm({ shipmentReference: shipment.shipmentReference || '', origin: shipment.origin || '', destination: shipment.destination || '', storageProfileId: shipment.storageProfileId || '', expectedDeliveryAt: shipment.expectedDeliveryAt ? toLocalDateTime(shipment.expectedDeliveryAt) : '' }); setEditOpen(true); }}>Edit shipment</button>}
      </div>
      {error && <div className="alert-card error">{error}</div>}
      {shipment.archivedAt && <div className="attention-banner"><strong>Shipment archived</strong><span>It remains available for traceability, but Operations can no longer add lots or temperature readings.</span></div>}
      {result?.condition && <div className={`reading-result ${result.dataQuality === 'DUPLICATE' ? 'warning' : ''}`} role="status"><Thermometer size={17} /><div><strong>{result.temperatureC}°C · {human(result.condition)}</strong><span>{result.message || (result.dataQuality === 'VALID' ? 'Reading saved and evaluated' : `Reading saved · ${human(result.dataQuality)}`)}</span></div></div>}
      {result?.notice && <div className="success-message">{result.notice}</div>}

      <section className="reading-summary-grid">
        <div className={`current-condition-card ${String(currentCondition).toLowerCase()}`}><span className="summary-label">Current condition</span><strong>{human(currentCondition)}</strong><span>{latestReading ? `Last reading ${fmt(latestReading.eventTime)}` : 'No temperature reading yet'}</span></div>
        <div className="summary-card"><span className="summary-label">Storage type</span><strong>{shipment.storageType || shipment.storageProfileName || '—'}</strong><span>Tracking {shipment.trackingNumber}</span></div>
        <div className="summary-card"><span className="summary-label">Lots on shipment</span><strong>{lots.length}</strong><span>{lots.filter(l => l.dispositionStatus === 'PENDING').length} awaiting disposition</span></div>
      </section>

      <section className="content-card">
        <div className="section-heading"><div><h2>Temperature readings</h2><p>Recent readings and their conditions</p></div>{isOperations && <button className="btn-primary" onClick={() => { setResult(null); setError(''); setEventTime(toLocalDateTime(Date.now())); setReadingOpen(true); }} disabled={activeLots.length === 0 || Boolean(shipment.archivedAt) || Boolean(shipment.deletedAt)}><Thermometer size={16} /> Add reading</button>}</div>
        {missing.length > 0 && <div className="attention-banner"><strong>{missing.length} missing reading interval{missing.length === 1 ? '' : 's'}</strong><span>Conditions during these periods are unknown.</span></div>}
        <TemperatureTimeline events={events} missingIntervals={missing} profile={shipment} />
        {events.length === 0 ? <div className="empty-inline">No readings have been recorded for this shipment.</div> : (
          <div className="table-scroll readings-table-wrap"><table className="business-table"><thead><tr><th>Recorded</th><th>Lot</th><th>Temperature</th><th>Condition</th><th>Data quality</th></tr></thead><tbody>
            {[...events].sort((a, b) => new Date(b.eventTime) - new Date(a.eventTime)).slice(0, 10).map((reading, index) => <tr key={reading.eventId || index}><td>{fmt(reading.eventTime)}</td><td>{reading.lotId || 'Shipment reading'}</td><td><strong>{reading.temperatureC}°C</strong></td><td><Condition condition={reading.condition} /></td><td><Quality value={reading.dataQuality} /></td></tr>)}
          </tbody></table></div>
        )}
      </section>

      <section className="content-card">
        <div className="section-heading"><div><h2>Lots</h2><p>Products travelling in this shipment</p></div>{isOperations && !shipment.archivedAt && <button className="btn-secondary" onClick={() => { setLotForm(emptyLotForm()); setLotOpen(true); }}><Plus size={16} /> Add lot</button>}</div>
        {lots.length === 0 ? <div className="empty-inline">No lots have been registered yet.</div> : (
          <div className="table-scroll"><table className="business-table"><thead><tr><th>Lot</th><th>Product</th><th>Lot number</th><th>Expiry</th><th>Quantity</th><th>Disposition</th>{isOperations && <th />}</tr></thead><tbody>
            {lots.map(lot => <tr key={lot.id}><td className="primary-cell">{lot.lotId || lot.id}{lot.archivedAt && <span className="secondary-cell">Archived</span>}</td><td>{lot.productName}</td><td>{lot.batchNumber}</td><td>{lot.expiryDate || '—'}</td><td>{lot.quantity.toLocaleString()}</td><td><StatusBadge status={lot.dispositionStatus} /></td>{isOperations && <td>{!shipment.archivedAt && !lot.archivedAt && <button className="btn-secondary compact" disabled={busy} onClick={() => removeOrArchiveLot(lot)}>{lot.hasOperationalHistory ? <><Archive size={14}/> Archive</> : <><Trash2 size={14}/> Delete</>}</button>}</td>}</tr>)}
          </tbody></table></div>
        )}
      </section>

      {issues.length > 0 && <section className="content-card"><div className="section-heading"><div><h2>Temperature issues</h2><p>Spikes or drifts linked to this shipment</p></div></div>
        <div className="issue-list">{issues.map(issue => <div className="issue-row" key={issue.id}>
          <div className="issue-symbol"><ClipboardList size={17} /></div><div className="issue-main"><strong>{human(issue.severity)} issue</strong><span>Peak {issue.peakTemperatureC}°C · {human(issue.status)}</span></div>
          <div className="issue-actions">{isOperations && (issue.affectedLotInternalIds?.includes(selectedLotId) || issue.affectedLotIds?.includes(lots.find(lot => lot.id === selectedLotId)?.lotId)) && (investigationsByIssue[issue.id] ? <Link className="btn-secondary compact" to={`/issues/${issue.id}`}>Open investigation <ArrowRight size={14}/></Link> : <button className="btn-secondary compact" disabled={busy} onClick={() => navigate(`/issues/${issue.id}?register=1`)}>Register investigation <ArrowRight size={14}/></button>)}<Link className="text-link" to={`/issues/${issue.id}`}>Review issue <ArrowRight size={15} /></Link></div>
        </div>)}</div>
      </section>}

      <EntityActivityTimeline entityType="SHIPMENT" entityId={shipment.id} />

      {readingOpen && <Modal title="Add temperature reading" onClose={() => setReadingOpen(false)}>
        <form onSubmit={recordTemperature} className="form-stack"><Field label="Lot"><select className="form-select full-width" required value={selectedLotId} onChange={e => setSelectedLotId(e.target.value)}>{activeLots.map(l => <option key={l.id} value={l.id}>{l.lotId || l.id} · {l.productName}</option>)}</select></Field>
          {activeSensors.length > 1 && <Field label="Sensor"><select className="form-select full-width" required value={selectedSensorId} onChange={e => setSelectedSensorId(e.target.value)}>{activeSensors.map(sensor => <option key={sensor.id} value={sensor.id}>{sensor.model} · {sensor.id}</option>)}</select></Field>}
          <Field label="Temperature (°C)"><input className="form-input" type="number" step="0.1" min="-100" max="100" required value={temperature} onChange={e => setTemperature(e.target.value)} placeholder="e.g. 5.4" autoFocus /></Field>
          <Field label="Event time"><input className="form-input" type="datetime-local" required value={eventTime} onChange={e => setEventTime(e.target.value)} /></Field>
          <p className="form-hint">The reading will be saved to this shipment and lot and evaluated against its storage conditions.</p>
          <div className="modal-footer"><button className="btn-secondary" type="button" onClick={() => setReadingOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save reading'}</button></div>
        </form>
      </Modal>}

      {lotOpen && <Modal title="Add lot to shipment" onClose={() => setLotOpen(false)}><form onSubmit={registerLot} className="form-stack">
        <Field label="Product"><input className="form-input" required value={lotForm.productName} onChange={e => setLotForm({ ...lotForm, productName: e.target.value })} placeholder="Medicine or product name" /></Field>
        <div className="form-grid"><Field label="Lot number"><input className="form-input" required value={lotForm.batchNumber} onChange={e => setLotForm({ ...lotForm, batchNumber: e.target.value })} /></Field><Field label="Quantity"><input className="form-input" type="number" min="1" required value={lotForm.quantity} onChange={e => setLotForm({ ...lotForm, quantity: e.target.value })} /></Field><Field label="Expiry date"><input className="form-input" type="date" value={lotForm.expiryDate} onChange={e => setLotForm({ ...lotForm, expiryDate: e.target.value })} /></Field></div>
        <div className="modal-footer"><button className="btn-secondary" type="button" onClick={() => setLotOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Add lot'}</button></div>
      </form></Modal>}
      {editOpen && <Modal title="Edit shipment details" onClose={() => setEditOpen(false)}><form onSubmit={updateShipment} className="form-stack">
        <Field label="Shipment reference"><input className="form-input" required value={editForm.shipmentReference} onChange={e => setEditForm({ ...editForm, shipmentReference: e.target.value })} /></Field>
        <div className="form-grid"><Field label="Origin"><input className="form-input" required value={editForm.origin} onChange={e => setEditForm({ ...editForm, origin: e.target.value })} /></Field><Field label="Destination"><input className="form-input" required value={editForm.destination} onChange={e => setEditForm({ ...editForm, destination: e.target.value })} /></Field></div>
        <Field label="Storage condition"><select className="form-select full-width" value={editForm.storageProfileId} onChange={e => setEditForm({ ...editForm, storageProfileId: e.target.value })}><option value="PROFILE-REF">Refrigerated · 2–8°C</option><option value="PROFILE-COOL">Cool · 8–15°C</option><option value="PROFILE-CRT">Controlled room · 15–25°C</option><option value="PROFILE-FRZ">Frozen · −25 to −10°C</option></select></Field>
        <Field label="Expected delivery"><input className="form-input" type="datetime-local" value={editForm.expectedDeliveryAt} onChange={e => setEditForm({ ...editForm, expectedDeliveryAt: e.target.value })} /></Field>
        <div className="modal-footer"><button className="btn-secondary" type="button" onClick={() => setEditOpen(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button></div>
      </form></Modal>}
    </div>
  );
}

function Field({ label, children }) { return <label className="form-field"><span>{label}</span>{children}</label>; }
function Condition({ condition }) { const value = String(condition || 'UNKNOWN').toUpperCase(); const tone = value === 'NORMAL' ? 'normal' : value === 'UNKNOWN' ? 'unknown' : value === 'AT_RISK' ? 'at-risk' : 'alert'; return <span className={`condition-label ${tone}`}><i />{human(value)}</span>; }
function Quality({ value }) { const quality = String(value || 'UNKNOWN').toUpperCase(); return <span className={`quality-label ${quality === 'VALID' ? 'valid' : 'other'}`}>{human(quality)}</span>; }
