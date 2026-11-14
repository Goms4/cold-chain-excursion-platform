import React, { useCallback, useEffect, useState } from 'react';
import { AlertTriangle, ClipboardList, Thermometer } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiRequest, getActiveRole } from '../api/client';
import SeverityMarker from '../components/SeverityMarker';
import StatusBadge from '../components/StatusBadge';
import DispositionModal from '../components/DispositionModal';
import EntityActivityTimeline from '../components/EntityActivityTimeline';
import Modal from '../components/Modal';
import { FormRow, MasterDetail, PageHeader } from '../components/EditorialUI';
import EvidenceComparisonCard from '../components/EvidenceComparisonCard';

const human = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function Investigation() {
  const role = getActiveRole();
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [decisions, setDecisions] = useState([]);
  const [sensorEvents, setSensorEvents] = useState([]);
  const [filter, setFilter] = useState('OPEN');
  const [busy, setBusy] = useState(false);
  const [assignee, setAssignee] = useState('');
  const [showDecision, setShowDecision] = useState(false);
  const [showEvidence, setShowEvidence] = useState(false);
  const [form, setForm] = useState({ inspectorName: '', inspectionTime: new Date().toISOString(), observedTemperatureC: '', packageIntegrityStatus: 'INTACT', visualCondition: '', notes: '' });
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setError('');
      const excursions = await apiRequest('/api/excursions');
      const responses = await Promise.all((excursions || []).map(exc => apiRequest(`/api/investigations/excursion/${exc.id}`).then(inv => ({ ...inv, issue: exc })).catch(() => null)));
      const list = responses.filter(Boolean);
      setItems(list);
      setSelectedId(current => list.some(i => (i.investigationId || i.id) === current) ? current : (list[0]?.investigationId || list[0]?.id || ''));
    } catch (e) { setError(e.message || 'Unable to load investigations.'); }
  }, []);
  useEffect(() => { load(); window.addEventListener('cold_chain_refresh', load); return () => window.removeEventListener('cold_chain_refresh', load); }, [load]);

  const selected = items.find(i => (i.investigationId || i.id) === selectedId) || items[0];
  useEffect(() => {
    if (!selected) { setDecisions([]); setSensorEvents([]); return; }
    apiRequest(`/api/investigations/${selected.investigationInternalId || selected.id}/decisions`).then(d => setDecisions(d || [])).catch(() => setDecisions([]));
    apiRequest(`/api/shipments/${selected.shipmentId}/sensor-events`).then(events => setSensorEvents(events || [])).catch(() => setSensorEvents([]));
  }, [selected?.investigationId, selected?.id, selected?.shipmentId]);
  const visible = items.filter(item => filter === 'ALL' || (filter === 'OPEN' ? item.status !== 'COMPLETED' : item.status === 'COMPLETED'));

  const addEvidence = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try {
      await apiRequest(`/api/investigations/${selected.investigationInternalId || selected.id}/manual-inspection`, { method: 'POST', body: JSON.stringify({ ...form, inspectorName: form.inspectorName || 'Operations', observedTemperatureC: Number(form.observedTemperatureC) }) });
      setShowEvidence(false); await load();
    } catch (e) { setError(e.message || 'Unable to add evidence.'); }
    finally { setBusy(false); }
  };
  const assignInvestigator = async () => {
    if (!selected || !assignee.trim()) return;
    setBusy(true); setError('');
    try {
      await apiRequest(`/api/investigations/${selected.investigationInternalId || selected.id}/assignee`, { method: 'PATCH', body: JSON.stringify({ investigator: assignee.trim() }) });
      setAssignee(''); await load();
    } catch (e) { setError(e.message || 'Unable to assign this investigation.'); }
    finally { setBusy(false); }
  };

  return <div className="page-stack">
    <PageHeader breadcrumb="Quality › Investigations" title="Investigations" summary="Review temperature issues, evidence and shipment decisions." actions={<select className="form-select" value={filter} onChange={e=>setFilter(e.target.value)}><option value="OPEN">Open investigations</option><option value="ALL">All investigations</option><option value="DONE">Completed</option></select>} />
    {error && <div className="alert-card error">{error}</div>}
    {visible.length === 0 ? <section className="content-card empty-state"><span className="empty-icon"><ClipboardList size={20}/></span><strong>No investigations to show</strong><span>When a temperature issue is raised, its investigation will appear here.</span></section> : (
      <MasterDetail>
        <div className="panel-top-strip master-detail-strip"><span>Investigations › {selected?.investigationId || 'Open items'}</span><span className={items.filter(i=>i.status!=='COMPLETED').length ? 'problem-count' : ''}>{items.filter(i=>i.status!=='COMPLETED').length ? `● ${items.filter(i=>i.status!=='COMPLETED').length} to look at` : 'No open investigations'}</span></div>
        <aside className="content-card investigation-list">{visible.map(item=>{
          const key=item.investigationId||item.id; const active=key===(selected?.investigationId||selected?.id);
          return <button key={key} className={`investigation-list-item ${active?'selected':''}`} onClick={()=>setSelectedId(key)}><span className="investigation-list-top"><SeverityMarker severity={item.severity || item.issue?.severity}/><StatusBadge status={item.status}/></span><strong>{item.shipmentId}</strong><span>{item.investigationId||item.id}</span><small>Started {fmt(item.startedAt||item.openedAt)}</small></button>;
        })}</aside>
        {selected && <section className="content-card investigation-detail">
          <div className="section-heading"><div><span className="eyebrow">{selected.investigationId||selected.id}</span><h2>{selected.shipmentId}</h2><p>{human(selected.issueType)} · {human(selected.status)}</p></div><div className="button-row">{role==='OPERATIONS' && <button className="btn-secondary" onClick={()=>setShowEvidence(true)}><ClipboardList size={16}/> Add evidence</button>}{role==='ANALYST' && selected.status!=='COMPLETED' && <button className="btn-primary" onClick={()=>setShowDecision(true)}>Make shipment decision</button>}</div></div>
          <div className="investigation-stage-line"><span>Detected</span><span>›</span><span>Investigation opened</span><span>›</span><span>{(selected.manualInspections||[]).length ? 'Evidence recorded' : 'Evidence pending'}</span><span>›</span><span>{decisions.length ? 'Decision recorded' : 'Decision pending'}</span></div>
          <div className="investigation-facts"><div><span>Severity</span><strong><SeverityMarker severity={selected.severity||selected.issue?.severity}/></strong></div><div><span>Peak temperature</span><strong>{selected.peakTemperatureC ?? selected.issue?.peakTemperatureC ?? '—'}°C</strong></div><div><span>Time out of range</span><strong>{selected.durationMinutes ?? selected.issue?.durationMinutes ?? 0} min</strong></div><div><span>Assigned investigator</span><strong>{selected.investigator || selected.assignedInvestigator || 'Unassigned'}</strong></div></div>
          {role === 'ANALYST' && selected.status !== 'COMPLETED' && <div className="investigator-assignment"><label className="search-box"><input value={assignee} onChange={event => setAssignee(event.target.value)} placeholder="Name of investigator" aria-label="Investigator name" /></label><button className="btn-secondary compact" disabled={busy || !assignee.trim()} onClick={assignInvestigator}>Assign investigator</button></div>}
          <div className="section-subheading"><h3>Affected lots</h3><Link className="text-link" to={`/shipments/${selected.shipmentInternalId || selected.shipmentId}`}>Open shipment</Link></div>
          <div className="lot-chip-list">{(selected.issue?.affectedLotIds||[]).map(lot=><span className="lot-chip" key={lot}>{lot}</span>)}</div>
          <div className="section-subheading"><h3>Operational evidence</h3></div>
          {(selected.manualInspections||[]).length===0 ? <p className="muted-copy">No additional evidence has been recorded.</p> : <div className="evidence-list">{selected.manualInspections.map((e,i)=><div className="evidence-row" key={`${e.inspectionTime}-${i}`}><div className="evidence-icon"><Thermometer size={17}/></div><div><strong>{e.observedTemperatureC}°C manual reading</strong><span>{e.inspectorName} · {fmt(e.inspectionTime)} · Package {human(e.packageIntegrityStatus)}</span>{e.visualCondition&&<p>{e.visualCondition}</p>}{e.notes&&<p>{e.notes}</p>}</div>{e.contradictsSensor&&<span className="quality-label other">Evidence differs</span>}</div>)}</div>}
          <EvidenceComparisonCard events={sensorEvents} manualInspections={selected.manualInspections || []} />
          {decisions.length>0 && <div className="decision-outcome"><span>Recorded shipment decision</span><strong>{decisions[decisions.length-1].decision}</strong><p>{decisions[decisions.length-1].reason}</p></div>}
          {selected.issue && <div className="inline-issue-link"><AlertTriangle size={16}/><span>Temperature issue on this shipment</span><Link className="text-link" to={`/issues/${selected.issue.id}`}>Review issue</Link></div>}
          <EntityActivityTimeline entityType="INVESTIGATION" entityId={selected.investigationInternalId || selected.id} />
        </section>}
      </MasterDetail>
    )}
    {showDecision&&<DispositionModal investigationId={selected.investigationInternalId||selected.id} lotIds={selected.issue?.affectedLotIds||[]} onClose={()=>setShowDecision(false)} onSuccess={async()=>{setShowDecision(false);await load();window.dispatchEvent(new CustomEvent('cold_chain_refresh'));}}/>}
    {showEvidence&&<Modal title="Add operational evidence" onClose={()=>setShowEvidence(false)}><form onSubmit={addEvidence} className="form-stack"><div className="form-grid"><Field label="Observed temperature (°C)"><input className="form-input" type="number" step="0.1" required value={form.observedTemperatureC} onChange={e=>setForm({...form,observedTemperatureC:e.target.value})}/></Field><Field label="Package integrity"><select className="form-select full-width" value={form.packageIntegrityStatus} onChange={e=>setForm({...form,packageIntegrityStatus:e.target.value})}><option>INTACT</option><option>DAMAGED</option><option>UNKNOWN</option></select></Field></div><Field label="What did you observe?"><textarea className="form-textarea" rows="3" value={form.visualCondition} onChange={e=>setForm({...form,visualCondition:e.target.value})} placeholder="Describe packaging or product condition"/></Field><Field label="Notes"><textarea className="form-textarea" rows="3" value={form.notes} onChange={e=>setForm({...form,notes:e.target.value})}/></Field>{error&&<div className="inline-error" role="alert">{error}</div>}<div className="modal-footer"><button type="button" className="btn-secondary" onClick={()=>setShowEvidence(false)}>Cancel</button><button className="btn-primary" disabled={busy}>{busy?'Saving…':'Save evidence'}</button></div></form></Modal>}
  </div>;
}
function Field({label,children}){return <FormRow label={label}>{children}</FormRow>}
