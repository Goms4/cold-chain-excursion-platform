import React, { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, ClipboardCheck, Thermometer } from 'lucide-react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { apiRequest, getActiveRole } from '../api/client';
import TemperatureTimeline from '../components/TemperatureTimeline';
import SeverityMarker from '../components/SeverityMarker';
import StatusBadge from '../components/StatusBadge';
import DispositionModal from '../components/DispositionModal';
import EntityActivityTimeline from '../components/EntityActivityTimeline';
import Modal from '../components/Modal';
import { DataTable, FormRow, MissingBox, NumberedSteps, PageHeader, Panel } from '../components/EditorialUI';

const human = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function ExcursionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const role = getActiveRole();
  const [issue, setIssue] = useState(null);
  const [events, setEvents] = useState([]);
  const [missingIntervals, setMissingIntervals] = useState([]);
  const [shipmentLots, setShipmentLots] = useState([]);
  const [investigation, setInvestigation] = useState(null);
  const [decisions, setDecisions] = useState([]);
  const [profile, setProfile] = useState(null);
  const [showDecision, setShowDecision] = useState(false);
  const [showEvidence, setShowEvidence] = useState(false);
  const [showRegistration, setShowRegistration] = useState(false);
  const [showAssignment, setShowAssignment] = useState(false);
  const [registrationLotId, setRegistrationLotId] = useState('');
  const [assignee, setAssignee] = useState('');
  const [busy, setBusy] = useState(false);
  const [loadComplete, setLoadComplete] = useState(false);
  const [evidence, setEvidence] = useState({ inspectorName: '', inspectionTime: new Date().toISOString(), observedTemperatureC: '', packageIntegrityStatus: 'INTACT', visualCondition: '', notes: '' });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    try {
      setLoadComplete(false);
      setError('');
      const current = await apiRequest(`/api/excursions/${id}`);
      const shipment = await apiRequest(`/api/shipments/${current.shipmentInternalId || current.shipmentId}`).catch(() => null);
      const nextProfile = shipment ? {
        targetMinC: shipment.targetMinC,
        targetMaxC: shipment.targetMaxC,
        minorMaxC: shipment.minorMaxC,
        criticalMaxC: shipment.criticalMaxC,
      } : null;
      const lotData = await apiRequest(`/api/shipments/${current.shipmentInternalId || current.shipmentId}/lots`).catch(() => []);
      const [readingData, inv, gapData] = await Promise.all([
        apiRequest(`/api/shipments/${current.shipmentInternalId || current.shipmentId}/sensor-events`).catch(() => []),
        apiRequest(`/api/investigations/excursion/${id}`).catch(() => null),
        apiRequest(`/api/shipments/${current.shipmentInternalId || current.shipmentId}/missing-intervals`).catch(() => []),
      ]);
      const decisionData = inv ? await apiRequest(`/api/investigations/${inv.id}/decisions`).catch(() => []) : [];
      setIssue(current); setProfile(nextProfile); setShipmentLots(lotData || []);
      setEvents(readingData || []); setInvestigation(inv); setMissingIntervals(gapData || []); setDecisions(decisionData || []);
    } catch (e) { setError(e.message || 'Unable to load this temperature issue.'); }
    finally { setLoadComplete(true); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!loadComplete || !issue || new URLSearchParams(location.search).get('register') !== '1') return;
    if (role === 'OPERATIONS' && !investigation) {
      setRegistrationLotId(issue.affectedLotIds?.[0] || '');
      setShowRegistration(true);
    } else if (investigation) {
      setMessage(`Investigation ${investigation.investigationId || investigation.id} is already registered for this issue.`);
    }
    navigate(location.pathname, { replace: true });
  }, [investigation, issue, loadComplete, location.pathname, location.search, navigate, role]);

  const registerInvestigation = async event => {
    event.preventDefault();
    if (!issue || !registrationLotId) return;
    setBusy(true); setError('');
    try {
      const created = await apiRequest('/api/investigations', { method: 'POST', body: JSON.stringify({
        shipmentId: issue.shipmentId,
        lotId: registrationLotId,
        excursionId: issue.id,
      }) });
      setShowRegistration(false);
      setMessage(`Investigation ${created.investigationId || created.id} is registered for this issue.`);
      await load();
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) { setError(e.message || 'Could not register the investigation.'); }
    finally { setBusy(false); }
  };

  const assignInvestigator = async event => {
    event.preventDefault();
    if (!investigation || !assignee.trim()) return;
    setBusy(true); setError('');
    try {
      await apiRequest(`/api/investigations/${investigation.id}/assignee`, { method: 'PATCH', body: JSON.stringify({ investigator: assignee.trim() }) });
      setAssignee(''); setShowAssignment(false);
      setMessage('Investigator assigned.');
      await load();
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) { setError(e.message || 'Could not assign this investigation.'); }
    finally { setBusy(false); }
  };

  const saveEvidence = async event => {
    event.preventDefault();
    setBusy(true); setError('');
    try {
      await apiRequest(`/api/investigations/${investigation.id}/manual-inspection`, { method: 'POST', body: JSON.stringify({ ...evidence, observedTemperatureC: Number(evidence.observedTemperatureC), inspectorName: evidence.inspectorName || 'Operations' }) });
      setShowEvidence(false); setMessage('Evidence added to the investigation.'); await load();
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) { setError(e.message || 'Could not save this evidence.'); }
    finally { setBusy(false); }
  };

  if (!issue && !error) return <div className="loading-card">Loading temperature issue…</div>;
  if (!issue) return <div className="page-stack"><div className="alert-card error">{error}</div><button className="btn-secondary" onClick={() => navigate('/issues')}>Back to issues</button></div>;
  const evidenceRows = investigation?.manualInspections || [];
  const affectedReadings = events.filter(e => e.dataQuality !== 'DUPLICATE' && e.condition && e.condition !== 'NORMAL');
  const affectedLotIds = issue.affectedLotIds || [];
  const triggeringReadings = events.filter(e => e.dataQuality !== 'DUPLICATE'
    && (!e.lotId || affectedLotIds.includes(e.lotId))
    && e.condition && e.condition !== 'NORMAL');
  const triggeringReading = triggeringReadings.find(e => Number(e.temperatureC) === Number(issue.peakTemperatureC))
    || triggeringReadings.sort((a, b) => Math.abs(new Date(a.eventTime) - new Date(issue.startEventTime)) - Math.abs(new Date(b.eventTime) - new Date(issue.startEventTime)))[0];
  const observedTemperature = triggeringReading?.temperatureC ?? issue.peakTemperatureC ?? '';
  const investigatorName = investigation?.assignedInvestigator || investigation?.investigator || '';
  const isUnassigned = !investigatorName.trim() || investigatorName.toLowerCase() === 'unassigned';
  const openEvidence = () => {
    setEvidence({ inspectorName: '', inspectionTime: new Date().toISOString(), observedTemperatureC: observedTemperature, packageIntegrityStatus: 'INTACT', visualCondition: '', notes: '' });
    setShowEvidence(true);
  };
  const findingTime = issue.detectedAt || issue.startEventTime;
  const findingClock = findingTime ? new Date(findingTime).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' }) : 'time unavailable';
  const storySteps = [
    { title: 'Reads the readings', children: affectedReadings.length === 0 ? <MissingBox label="No readings" /> : <DataTable><thead><tr><th>Time</th><th>Temperature</th><th>Source</th></tr></thead><tbody>{affectedReadings.slice(0, 5).map((e, i) => <tr key={e.eventId || i}><td>{fmt(e.eventTime)}</td><td>{e.temperatureC}°C</td><td>{e.sensorId || 'Sensor'}</td></tr>)}</tbody></DataTable> },
    { title: 'Applies the rule', description: `Target ${profile?.targetMinC ?? '—'} to ${profile?.targetMaxC ?? '—'} °C · minor limit ${profile?.minorMaxC ?? '—'} °C · critical limit ${profile?.criticalMaxC ?? '—'} °C · ${human(issue.severity)} severity · rule ${issue.ruleVersion || 'current profile'}` },
    { title: 'Traces the lots', children: <div className="lot-trace-summary"><div><strong>Affected</strong><span>{(issue.affectedLotIds || []).length ? issue.affectedLotIds.join(', ') : 'No lots recorded'}</span></div><div><strong>Not affected</strong><span>{shipmentLots.filter(lot => lot.unloadedAt && issue.startEventTime && new Date(lot.unloadedAt) <= new Date(issue.startEventTime)).map(lot => lot.lotId || lot.id).join(', ') || 'No lots unloaded before this issue'}</span></div></div> },
    { title: `Flags it at ${findingClock}`, children: <div className="finding-block">Reached {issue.peakTemperatureC ?? 'unknown'} °C for {issue.durationMinutes ?? 0} min; {(issue.affectedLotIds || []).length ? `${issue.affectedLotIds.join(', ')} affected` : 'affected lots not recorded'}.</div> },
  ];

  return <div className="page-stack">
    <button className="back-link" onClick={() => navigate('/issues')}><ArrowLeft size={16} /> Temperature issues</button>
    <PageHeader breadcrumb={`Temperature issues › ${issue.id}`} title={issue.shipmentId} summary={`Detected ${fmt(issue.startEventTime)} · ${issue.affectedLotIds?.length || 0} affected lots`} status={<div className="detail-heading-status"><SeverityMarker severity={issue.severity} /><StatusBadge status={issue.status} /></div>} />
    {error && <div className="alert-card error">{error}</div>}{message && <div className="success-message">{message}</div>}
    <section className="issue-facts-grid">
      <div className="issue-fact"><span>Peak temperature</span><strong>{issue.peakTemperatureC}°C</strong></div>
      <div className="issue-fact"><span>Out-of-range duration</span><strong>{issue.durationMinutes ?? 0} min</strong></div>
      <div className="issue-fact"><span>Shipment</span><Link to={`/shipments/${issue.shipmentInternalId || issue.shipmentId}`}>{issue.shipmentId}</Link></div>
      <div className="issue-fact"><span>Investigation</span><strong>{investigation ? human(investigation.status) : 'Not registered'}</strong></div>
      <div className="issue-fact"><span>Unique readings</span><strong>{events.filter(e => e.dataQuality !== 'DUPLICATE').length} readings</strong></div>
    </section>
    <NumberedSteps steps={storySteps} />
    <Panel title="Temperature evidence"><p className="panel-intro">Readings show the observed pattern across this shipment</p><TemperatureTimeline events={events} missingIntervals={missingIntervals} profile={profile || undefined} /></Panel>
    <Panel title="Readings outside target">
      <p className="panel-intro">Relevant measurements associated with this issue</p>
      {affectedReadings.length === 0 ? <MissingBox label="No readings" /> : <DataTable><thead><tr><th>Time</th><th>Lot</th><th>Temperature</th><th>Condition</th><th>Data quality</th></tr></thead><tbody>{affectedReadings.map((e,i)=><tr key={e.eventId || i}><td>{fmt(e.eventTime)}</td><td>{e.lotId || 'Shipment reading'}</td><td><strong>{e.temperatureC}°C</strong></td><td>{human(e.condition)}</td><td>{human(e.dataQuality)}</td></tr>)}</tbody></DataTable>}
    </Panel>
    <section className="content-card"><div className="section-heading"><div><h2>Investigation and evidence</h2><p>Operational observations help assess product impact</p></div><div className="button-row">{role === 'OPERATIONS' && investigation && <button className="btn-secondary" onClick={openEvidence}><ClipboardCheck size={16} /> Add evidence</button>}{role === 'ANALYST' && investigation?.status !== 'COMPLETED' && <button className="btn-primary" onClick={() => setShowDecision(true)}>Record shipment decision</button>}</div></div>
      {!investigation ? <><div className="empty-inline">An investigation has not been registered for this issue.</div>{role === 'OPERATIONS' && <div className="investigation-actions"><button className="btn-primary" onClick={() => { setRegistrationLotId(issue.affectedLotIds?.[0] || ''); setShowRegistration(true); }}>Register investigation</button></div>}</> : <>
        <div className="investigation-summary"><div><span>Investigation ID</span><strong>{investigation.investigationId || investigation.id}</strong></div><div><span>Status</span><StatusBadge status={investigation.status} /></div><div><span>Assigned investigator</span><strong>{isUnassigned ? 'Unassigned' : investigatorName}</strong></div><div><span>Affected shipment / lot</span><strong>{issue.shipmentId} · {affectedLotIds.join(', ') || 'Not recorded'}</strong></div></div>
        {role === 'OPERATIONS' && isUnassigned && <div className="investigation-actions">{!showAssignment ? <button className="btn-secondary" onClick={() => setShowAssignment(true)}>Assign investigator</button> : <form className="investigator-assignment" onSubmit={assignInvestigator}><Field label="Investigator name"><input className="form-input" value={assignee} onChange={event => setAssignee(event.target.value)} autoFocus required /></Field><button className="btn-primary" disabled={busy || !assignee.trim()}>{busy ? 'Assigning…' : 'Confirm assignment'}</button><button type="button" className="btn-secondary" onClick={() => setShowAssignment(false)}>Cancel</button></form>}</div>}
        {evidenceRows.length > 0 && <div className="evidence-list">{evidenceRows.map((row,i)=><div className="evidence-row" key={`${row.inspectionTime}-${i}`}><div className="evidence-icon"><Thermometer size={17} /></div><div><strong>{row.observedTemperatureC}°C manual reading</strong><span>{row.inspectorName} · {fmt(row.inspectionTime)}</span>{row.notes && <p>{row.notes}</p>}</div>{row.contradictsSensor && <span className="quality-label other">Evidence differs</span>}</div>)}</div>}
        {decisions.length > 0 && <div className="decision-outcome"><span>Shipment decision</span><strong>{decisions[decisions.length - 1].decision}</strong><p>{decisions[decisions.length - 1].reason}</p></div>}
      </>}
    </section>
    <EntityActivityTimeline entityType="EXCURSION" entityId={issue.id} />
    {showDecision && <DispositionModal investigationId={investigation.id} lotIds={issue.affectedLotIds || []} onClose={() => setShowDecision(false)} onSuccess={async () => { setShowDecision(false); await load(); window.dispatchEvent(new CustomEvent('cold_chain_refresh')); }} />}
    {showRegistration && <Modal title="Register investigation" eyebrow={issue.id} onClose={() => setShowRegistration(false)}><form onSubmit={registerInvestigation} className="form-stack"><p className="modal-context">Registering this temperature issue for Operations review.</p><div className="registration-context"><div><span>Shipment</span><strong>{issue.shipmentId}</strong></div><div><span>Issue</span><strong>{issue.id} · {human(issue.severity)}</strong></div><div><span>Affected lot</span><strong>{registrationLotId || 'No affected lot recorded'}</strong></div><div><span>Triggering temperature</span><strong>{observedTemperature !== '' ? `${observedTemperature}°C` : 'Not available'}</strong></div></div>{affectedLotIds.length > 1 && <Field label="Affected lot"><select className="form-select full-width" required value={registrationLotId} onChange={event => setRegistrationLotId(event.target.value)}>{affectedLotIds.map(lotId => <option key={lotId} value={lotId}>{lotId}</option>)}</select></Field>}{affectedLotIds.length === 0 && <div className="inline-error" role="alert">This issue has no affected lot recorded, so it cannot be registered through the existing investigation API.</div>}{error && <div className="inline-error" role="alert">{error}</div>}<div className="modal-footer"><button type="button" className="btn-secondary" onClick={() => setShowRegistration(false)}>Cancel</button><button className="btn-primary" disabled={busy || !registrationLotId}>{busy ? 'Registering…' : 'Register investigation'}</button></div></form></Modal>}
    {showEvidence && <Modal title="Add operational evidence" eyebrow={`${investigation.investigationId || investigation.id} · ${issue.shipmentId}`} onClose={() => setShowEvidence(false)}><form onSubmit={saveEvidence} className="form-stack"><p className="modal-context">Investigation {investigation.investigationId || investigation.id} · {issue.shipmentId}</p><div className="form-grid"><Field label="Observed temperature (°C)"><input className="form-input" type="number" step="0.1" required value={evidence.observedTemperatureC} onChange={e=>setEvidence({...evidence,observedTemperatureC:e.target.value})}/></Field><Field label="Package integrity"><select className="form-select full-width" value={evidence.packageIntegrityStatus} onChange={e=>setEvidence({...evidence,packageIntegrityStatus:e.target.value})}><option>INTACT</option><option>DAMAGED</option><option>UNKNOWN</option></select></Field></div><Field label="Observation"><textarea className="form-textarea" rows="3" value={evidence.visualCondition} onChange={e=>setEvidence({...evidence,visualCondition:e.target.value})} placeholder="Describe packaging or product condition"/></Field><Field label="Notes"><textarea className="form-textarea" rows="3" value={evidence.notes} onChange={e=>setEvidence({...evidence,notes:e.target.value})}/></Field>{error && <div className="inline-error" role="alert">{error}</div>}<div className="modal-footer"><button type="button" className="btn-secondary" onClick={()=>setShowEvidence(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save evidence'}</button></div></form></Modal>}
  </div>;
}
function Field({label,children}){return <FormRow label={label}>{children}</FormRow>}
