import React, { useCallback, useEffect, useState } from 'react';
import { ArrowRight, AlertTriangle, CheckCircle2, ClipboardList, Package } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiRequest, getActiveRole } from '../api/client';
import { DataTable, PageHeader, Panel } from '../components/EditorialUI';
import StatusBadge from '../components/StatusBadge';

const human = value => String(value || 'Unknown').replace(/_/g, ' ').toLowerCase().replace(/^./, c => c.toUpperCase());
const timeAgo = value => {
  if (!value) return '—';
  const mins = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
  if (mins < 60) return `${mins} min ago`;
  if (mins < 1440) return `${Math.floor(mins / 60)} hr ago`;
  return `${Math.floor(mins / 1440)} days ago`;
};

export default function Dashboard() {
  const role = getActiveRole();
  const [shipments, setShipments] = useState([]);
  const [issues, setIssues] = useState([]);
  const [investigations, setInvestigations] = useState([]);
  const [summary, setSummary] = useState(null);
  const [reconciliation, setReconciliation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      const [shipmentData, issueData, summaryData, reconciliationData] = await Promise.all([
        apiRequest('/api/shipments'),
        apiRequest('/api/excursions'),
        apiRequest('/api/dashboard/summary'),
        apiRequest('/api/reconciliation/summary'),
      ]);
      const openIssues = (issueData || []).filter(i => !['CLOSED', 'DISPOSITIONED'].includes(i.status));
      const investigationData = await Promise.all(openIssues.map(issue =>
        apiRequest(`/api/investigations/excursion/${issue.id}`).catch(() => null)
      ));
      setShipments(shipmentData || []);
      setIssues(openIssues);
      setInvestigations(investigationData.filter(i => i && i.status !== 'COMPLETED'));
      setSummary(summaryData);
      setReconciliation(reconciliationData);
    } catch (e) {
      setError(e.message || 'Unable to load shipment information.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    window.addEventListener('cold_chain_refresh', load);
    return () => window.removeEventListener('cold_chain_refresh', load);
  }, [load]);

  const needsAttention = shipments.filter(s => ['TEMPERATURE_ISSUE', 'AT_RISK'].includes(s.condition));
  const latestShipments = [...shipments].sort((a, b) => new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0)).slice(0, 6);

  return (
    <div className="page-stack">
      <PageHeader breadcrumb={role === 'ANALYST' ? 'Quality › Overview' : 'Operations › Overview'} title={role === 'ANALYST' ? 'Shipment health' : 'Shipment operations'} summary={role === 'ANALYST' ? 'Review emerging issues and decide what happens next.' : 'See what is moving and which shipments need attention.'} actions={<button className="btn-secondary" onClick={load}>Refresh</button>} />

      {error && <div className="alert-card error" role="alert">{error}</div>}

      <section className="metric-grid" aria-label="Shipment summary">
        <Link className="metric-link" to="/shipments"><Metric icon={Package} label="Shipments in view" value={summary?.activeShipments ?? shipments.length} detail="Across active routes" /></Link>
        <Link className="metric-link" to="/shipments?filter=attention"><Metric icon={AlertTriangle} label="Need attention" value={summary?.needAttention ?? needsAttention.length} detail="Temperature issues and at-risk shipments" accent={(summary?.needAttention ?? needsAttention.length) > 0} /></Link>
        <Link className="metric-link" to="/investigations"><Metric icon={ClipboardList} label={role === 'ANALYST' ? 'Open investigations' : 'Issues being reviewed'} value={summary?.openInvestigations ?? investigations.length} detail="Awaiting investigation progress" /></Link>
      </section>

      {loading ? <div className="loading-card">Loading shipment overview…</div> : (
        <>
        <div className="dashboard-grid">
          <section className="content-card">
            <div className="section-heading">
              <div><h2>{role === 'ANALYST' ? 'Issues requiring review' : 'Needs attention'}</h2><p>Temperature conditions outside the shipment target</p></div>
              <Link to="/issues" className="text-link">View all <ArrowRight size={15} /></Link>
            </div>
            {issues.length === 0 ? <EmptyState icon={CheckCircle2} title="No active temperature issues" detail="Shipments are currently within their monitored conditions." /> : (
              <div className="issue-list">
                {issues.slice(0, 6).map(issue => {
                  const peak = Number(issue.peakTemperatureC) || 0;
                  const peakMark = Math.max(4, Math.min(96, (peak / 50) * 100));
                  return <Link className="issue-row attention-card" key={issue.id} to={`/issues/${issue.id}`}>
                    <div className="issue-card-meta"><span>{issue.id}</span><span>°C</span></div>
                    <div className="issue-card-visual"><div><span>Peak reading</span><strong>{issue.peakTemperatureC ?? '—'}°C</strong></div><div className="attention-scale"><i style={{ left: `${peakMark}%` }} /></div></div>
                    <span className="issue-card-category">Temperature issue · {human(issue.severity)}</span>
                    <div className="issue-main"><strong>{issue.shipmentId}</strong><span>{issue.summary || `Outside the target range for ${issue.durationMinutes ?? 0} min`} · {timeAgo(issue.startEventTime)}</span></div>
                    <span className="attention-card-link">Review issue →</span>
                  </Link>;
                })}
              </div>
            )}
          </section>

          <section className="content-card">
            <div className="section-heading">
              <div><h2>Recent shipments</h2><p>Latest monitored shipments</p></div>
              <Link to="/shipments" className="text-link">All shipments <ArrowRight size={15} /></Link>
            </div>
            {latestShipments.length === 0 ? <EmptyState icon={Package} title="No shipments yet" detail="Registered shipments will appear here." /> : (
              <div className="shipment-list">
                {latestShipments.map(shipment => (
                  <Link className="shipment-row" key={shipment.id} to={`/shipments/${shipment.id}`}>
                    <div className="shipment-route"><strong>{shipment.shipmentId || shipment.id}</strong><span>{shipment.origin} <span aria-hidden="true">→</span> {shipment.destination}</span></div>
                    <div className="shipment-row-status"><Condition condition={shipment.condition} /><StatusBadge status={shipment.status} /></div>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>
        <Panel title="Data reconciliation" className="dashboard-reconciliation">
          <p className="panel-intro">Raw, processed and review records from the current data store</p>
          <DataTable><thead><tr><th>Measure</th><th>Records</th><th>Check</th></tr></thead><tbody>
            <tr><td>Raw sensor events</td><td>{reconciliation?.totalRawEvents ?? '—'}</td><td>Received</td></tr>
            <tr><td>Processed events</td><td>{reconciliation?.totalProcessedEvents ?? '—'}</td><td>{reconciliation?.reconciled ? 'Reconciled' : 'Check required'}</td></tr>
            <tr><td>Duplicates</td><td>{reconciliation?.totalDuplicateEvents ?? '—'}</td><td>Retained for audit</td></tr>
            <tr><td>Quarantined</td><td>{reconciliation?.totalQuarantinedEvents ?? '—'}</td><td>Review required</td></tr>
            <tr><td>Missing intervals</td><td>{reconciliation?.totalMissingIntervals ?? '—'}</td><td>Unknown data</td></tr>
          </tbody></DataTable>
        </Panel>
        </>
      )}
    </div>
  );
}

function Metric({ icon: Icon, label, value, detail, accent }) {
  return <div className={`metric-card ${accent ? 'attention' : ''}`}><div className="metric-icon"><Icon size={18} /></div><div className="metric-label">{label}</div><div className="metric-value">{value}</div><div className="metric-detail">{detail}</div></div>;
}
function Condition({ condition }) {
  const value = String(condition || 'UNKNOWN').toUpperCase();
  const tone = value === 'NORMAL' ? 'normal' : value === 'UNKNOWN' ? 'unknown' : value === 'AT_RISK' ? 'at-risk' : 'alert';
  return <span className={`condition-label ${tone}`}><span />{human(value)}</span>;
}
function EmptyState({ icon: Icon, title, detail }) {
  return <div className="empty-state"><span className="empty-icon"><Icon size={20} /></span><strong>{title}</strong><span>{detail}</span></div>;
}
