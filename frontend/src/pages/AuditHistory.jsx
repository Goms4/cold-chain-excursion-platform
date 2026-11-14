import React, { useCallback, useEffect, useState } from 'react';
import { ChevronDown, ChevronUp, History } from 'lucide-react';
import { apiRequest } from '../api/client';

const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';
const human = value => String(value || '').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());

export default function AuditHistory() {
  const [records, setRecords] = useState([]);
  const [technical, setTechnical] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [showTechnical, setShowTechnical] = useState(false);
  const [expanded, setExpanded] = useState('');
  const [error, setError] = useState('');
  const [chain, setChain] = useState(null);
  const load = useCallback(async () => {
    try {
      setError('');
      const result = await apiRequest('/api/audit/activity?page=0&size=100');
      setRecords(result.content || []);
      setChain(await apiRequest('/api/audit/verify'));
    } catch (e) { setError(e.message || 'Unable to load activity history.'); }
  }, []);
  const loadTechnical = useCallback(async () => {
    try {
      setTechnical(await apiRequest('/api/audit/technical/groups?page=0&size=100') || []);
    } catch (e) { setError(e.message || 'Unable to load technical audit details.'); }
  }, []);
  useEffect(() => { load(); window.addEventListener('cold_chain_refresh', load); return () => window.removeEventListener('cold_chain_refresh', load); }, [load]);
  useEffect(() => { if (showTechnical) loadTechnical(); }, [showTechnical, loadTechnical]);
  const filtered = filter === 'ALL' ? records : records.filter(record => record.entityType === filter);

  return <div className="page-stack">
    <div className="page-heading-row"><div><span className="eyebrow">ACCOUNTABILITY</span><h1>Activity history</h1><p>See who changed what, for which shipment, when and why.</p></div><select className="form-select" value={filter} onChange={e => setFilter(e.target.value)}><option value="ALL">All activity</option><option value="SHIPMENT">Shipments</option><option value="LOT">Lots</option><option value="SENSOR_EVENT">Temperature readings</option><option value="EXCURSION">Temperature issues</option><option value="INVESTIGATION">Investigations</option><option value="DECISION">Decisions</option></select></div>
    {error && <div className="alert-card error">{error}</div>}
    {chain && <p className={chain.ok ? 'audit-chain-status' : 'audit-chain-status broken'} role="status">{chain.ok ? `Chain verified: ${chain.rowsChecked} records, checked ${fmt(chain.checkedAt)}` : `Chain broken at record ${chain.firstBrokenSeq}`}</p>}
    <section className="content-card audit-list">
      {filtered.length === 0 ? <div className="empty-state"><span className="empty-icon"><History size={20}/></span><strong>No activity to show</strong><span>Recorded shipment, temperature and review actions appear here.</span></div> : filtered.map(record => {
        const key = record.id;
        const open = expanded === `business:${key}`;
        const reference = [record.shipmentId, record.investigationId, ...(record.lotIds || [])].filter(Boolean).join(' · ') || record.entityId;
        return <article className="audit-entry" key={key}>
          <button className="audit-entry-button" onClick={() => setExpanded(open ? '' : `business:${key}`)} aria-expanded={open}>
            <span className="audit-time"><span className="mono">#{record.seq}</span><br/>{fmt(record.timestamp)}</span>
            <span className="audit-action"><strong>{record.action}</strong><small>{record.summary}</small><small className="mono" title={`row_hash: ${record.rowHash}\nprev_hash: ${record.prevHash}`}>{record.rowHash?.slice(0, 8)}</small></span>
            <span className="audit-actor">{record.actor}</span>{open ? <ChevronUp size={16}/> : <ChevronDown size={16}/>}
          </button>
          {open && <div className="audit-details"><div><span>Related records</span><strong>{reference}</strong></div><div><span>Reason</span><p>{record.rationale || 'No additional reason was recorded.'}</p></div></div>}
        </article>;
      })}
    </section>
    <button className="text-link audit-technical-toggle" type="button" aria-pressed={showTechnical} onClick={() => { setShowTechnical(value => !value); setExpanded(''); }}>{showTechnical ? 'Hide technical events' : 'Show technical events'}</button>
    {showTechnical && <section className="content-card audit-list technical-audit-list" aria-label="Technical audit events">
      <div className="section-heading"><div><h2>Technical audit details</h2><p>Internal event references and processing notes</p></div></div>
      {technical.map(group => {
        const key = `technical:${group.id}`;
        const open = expanded === key;
        return <article className="audit-entry" key={group.id}>
          <button className="audit-entry-button" onClick={() => setExpanded(open ? '' : key)} aria-expanded={open}>
            <span className="audit-time">{fmt(group.latestAt)}</span><span className="audit-action"><strong>{group.summary}</strong><small>{group.count} event{group.count === 1 ? '' : 's'} · {human(group.action)}</small></span><span className="audit-actor">{group.shipmentId || 'System'}</span>{open ? <ChevronUp size={16}/> : <ChevronDown size={16}/>}
          </button>
          {open && <div className="audit-details">{group.entries.map(entry => <div key={entry.id}><span>{fmt(entry.timestamp)} · {entry.actor} · {entry.entityId}</span><p>{entry.details || 'No additional details were recorded.'}</p></div>)}</div>}
        </article>;
      })}
    </section>}
  </div>;
}
