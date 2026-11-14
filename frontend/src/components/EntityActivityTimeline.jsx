import React, { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';

const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function EntityActivityTimeline({ entityType, entityId }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!entityId) return;
    let active = true;
    const load = () => apiRequest(`/api/audit/entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}/activity`)
      .then(rows => { if (active) { setItems(rows || []); setError(''); } })
      .catch(e => { if (active) setError(e.message || 'Activity history is unavailable.'); });
    load();
    window.addEventListener('cold_chain_refresh', load);
    return () => { active = false; window.removeEventListener('cold_chain_refresh', load); };
  }, [entityType, entityId]);

  return <section className="content-card entity-activity" aria-label="Entity activity timeline">
    <div className="section-heading"><div><h2>Activity timeline</h2><p>Recorded actions related to this record</p></div></div>
    {error ? <p className="muted-copy">{error}</p> : items.length === 0 ? <p className="muted-copy">No recorded activity yet.</p> : <ol className="entity-activity-list">
      {items.map(item => <li key={item.id}><time dateTime={item.timestamp}>{fmt(item.timestamp)}</time><div><strong>{item.action}</strong><span>{item.summary}</span><small>{item.actor}{item.rationale ? ` · ${item.rationale}` : ''}</small></div></li>)}
    </ol>}
  </section>;
}
