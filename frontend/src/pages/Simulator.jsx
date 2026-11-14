import React, { useState } from 'react';
import { apiRequest } from '../api/client';

const SCENARIOS = [
  { id: '1', label: 'Normal shipment', expected: 'No excursion created' },
  { id: '2', label: 'Minor excursion', expected: 'Minor excursion detected' },
  { id: '3', label: 'Major / critical excursion', expected: 'Critical excursion detected' },
  { id: '4', label: 'Duplicate event', expected: 'Duplicate suppressed' },
  { id: '5', label: 'Out-of-order events', expected: 'Events evaluated by event_time' },
  { id: '6', label: 'Missing data and contradictory inspection', expected: 'Unknown gaps + contradictory evidence flagged' },
];

export default function Simulator() {
  const [results, setResults] = useState({});
  const [expanded, setExpanded] = useState({});
  const [runningId, setRunningId] = useState(null);

  const runScenario = async (scenarioId) => {
    setRunningId(scenarioId);
    try {
      const res = await apiRequest(`/api/scenarios/run/${scenarioId}`, { method: 'POST' });
      setResults(prev => ({ ...prev, [scenarioId]: res }));
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) {
      setResults(prev => ({
        ...prev,
        [scenarioId]: { scenarioName: scenarioId, success: false, verificationNotes: [e.message] },
      }));
    } finally {
      setRunningId(null);
    }
  };

  const runAll = async () => {
    setRunningId('all');
    try {
      const res = await apiRequest('/api/scenarios/run/all', { method: 'POST' });
      // res may be an array or single result
      if (Array.isArray(res)) {
        const map = {};
        res.forEach(r => { if (r.scenarioId || r.scenarioName) map[r.scenarioId || r.scenarioName] = r; });
        setResults(map);
      } else {
        setResults(prev => ({ ...prev, all: res }));
      }
      window.dispatchEvent(new CustomEvent('cold_chain_refresh'));
    } catch (e) {
      setResults(prev => ({ ...prev, all: { success: false, verificationNotes: [e.message] } }));
    } finally {
      setRunningId(null);
    }
  };

  const toggleExpand = (id) => setExpanded(prev => ({ ...prev, [id]: !prev[id] }));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text)' }}>Simulator</h1>
        <button
          className="btn-primary"
          onClick={runAll}
          disabled={runningId !== null}
        >
          {runningId === 'all' ? 'Running…' : 'Run all'}
        </button>
      </div>

      <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>
        Each scenario posts deterministic test data and verifies the platform response.
        Results are shown below.
      </div>

      {/* Scenarios table */}
      <div className="panel" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Scenario</th>
              <th>Expected</th>
              <th>Result</th>
              <th style={{ textAlign: 'right' }}>Run</th>
            </tr>
          </thead>
          <tbody>
            {SCENARIOS.map(sc => {
              const r = results[sc.id];
              const isRunning = runningId === sc.id;
              return (
                <React.Fragment key={sc.id}>
                  <tr>
                    <td style={{ fontSize: 13 }}>{sc.label}</td>
                    <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>{sc.expected}</td>
                    <td>
                      {!r ? (
                        <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>—</span>
                      ) : r.success ? (
                        <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--accent)' }}>Pass</span>
                      ) : (
                        <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--sev-critical)' }}>Fail</span>
                      )}
                      {r && r.verificationNotes?.length > 0 && (
                        <button
                          onClick={() => toggleExpand(sc.id)}
                          style={{ marginLeft: 8, background: 'none', border: 'none', cursor: 'pointer', fontSize: 11, color: 'var(--accent)' }}
                        >
                          {expanded[sc.id] ? 'hide' : 'details'}
                        </button>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn-secondary"
                        onClick={() => runScenario(sc.id)}
                        disabled={runningId !== null}
                        style={{ fontSize: 12 }}
                      >
                        {isRunning ? 'Running…' : 'Run'}
                      </button>
                    </td>
                  </tr>
                  {expanded[sc.id] && r?.verificationNotes?.length > 0 && (
                    <tr>
                      <td colSpan={4} style={{ padding: '8px 12px', background: '#FAFAFA' }}>
                        <pre style={{
                          margin: 0,
                          fontFamily: 'ui-monospace, monospace',
                          fontSize: 11,
                          color: 'var(--text-muted)',
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-all',
                        }}>
                          {r.verificationNotes.join('\n')}
                        </pre>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
