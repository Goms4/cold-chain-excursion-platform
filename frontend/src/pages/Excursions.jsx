import React, { useCallback, useEffect, useState } from 'react';
import { ArrowRight, CircleCheck, Thermometer } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiRequest } from '../api/client';
import SeverityMarker from '../components/SeverityMarker';
import StatusBadge from '../components/StatusBadge';

const human = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
const fmt = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function Excursions() {
  const [issues, setIssues] = useState([]);
  const [filter, setFilter] = useState('OPEN');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    try { setError(''); setIssues(await apiRequest('/api/excursions') || []); }
    catch (e) { setError(e.message || 'Could not load temperature issues.'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); window.addEventListener('cold_chain_refresh', load); return () => window.removeEventListener('cold_chain_refresh', load); }, [load]);
  const active = issues.filter(i => !['CLOSED', 'DISPOSITIONED'].includes(i.status));
  const visible = filter === 'OPEN' ? active : filter === 'RESOLVED' ? issues.filter(i => !active.includes(i)) : issues;

  return <div className="page-stack">
    <div className="page-heading-row"><div><span className="eyebrow">SHIPMENT HEALTH</span><h1>Temperature issues</h1><p>Spikes and drifts that may affect product quality.</p></div><button className="btn-secondary" onClick={load}>Refresh</button></div>
    {error && <div className="alert-card error">{error}</div>}
    <div className="issue-summary-strip"><span><Thermometer size={17} /> {active.length} active issue{active.length === 1 ? '' : 's'}</span><span>{issues.length - active.length} resolved</span></div>
    <section className="content-card">
      <div className="list-toolbar"><div><h2>Temperature issues</h2><p>Review affected shipments and investigation status</p></div><select className="form-select" value={filter} onChange={e => setFilter(e.target.value)}><option value="OPEN">Active issues</option><option value="ALL">All issues</option><option value="RESOLVED">Resolved issues</option></select></div>
      {loading ? <div className="loading-card">Loading temperature issues…</div> : visible.length === 0 ? <div className="empty-state"><span className="empty-icon"><CircleCheck size={20} /></span><strong>{filter === 'RESOLVED' ? 'No resolved issues yet' : 'No active temperature issues'}</strong><span>Any spike or drift detected on monitored shipments will appear here.</span></div> : (
        <div className="table-scroll"><table className="business-table"><thead><tr><th>Shipment</th><th>Issue</th><th>Peak temperature</th><th>Duration</th><th>Affected lots</th><th>Review status</th><th /></tr></thead><tbody>
          {visible.map(issue => <tr key={issue.id}>
            <td><Link className="primary-cell" to={`/shipments/${issue.shipmentInternalId || issue.shipmentId}`}>{issue.shipmentId}</Link><span className="secondary-cell">Started {fmt(issue.startEventTime)}</span></td>
            <td><SeverityMarker severity={issue.severity} /><span className="secondary-cell">{issue.summary || 'Temperature excursion detected.'}</span></td><td><strong>{issue.peakTemperatureC}°C</strong></td><td>{issue.readingCount === 1 ? '1 reading' : `${issue.durationMinutes || 0} min`}</td>
            <td>{issue.affectedLotIds?.join(', ') || '—'}</td><td><StatusBadge status={issue.status} /></td><td><Link className="icon-link" to={`/issues/${issue.id}`} aria-label="Review issue"><ArrowRight size={17} /></Link></td>
          </tr>)}
        </tbody></table></div>
      )}
    </section>
  </div>;
}
