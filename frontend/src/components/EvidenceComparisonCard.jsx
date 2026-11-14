import React from 'react';
import { DataTable, MissingBox } from './EditorialUI';

const formatTime = value => value ? new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function EvidenceComparisonCard({ events = [], manualInspections = [] }) {
  const sensor = [...events].filter(row => row.dataQuality !== 'DUPLICATE').sort((a, b) => new Date(b.eventTime) - new Date(a.eventTime))[0];
  const manual = [...manualInspections].sort((a, b) => new Date(b.inspectionTime) - new Date(a.inspectionTime))[0];
  const contradictory = manualInspections.some(row => row.contradictsSensor);
  const rows = [sensor && { time: sensor.eventTime, kind: 'Sensor', value: `${sensor.temperatureC} °C`, source: sensor.sensorId || 'Sensor event' }, manual && { time: manual.inspectionTime, kind: 'Manual inspection', value: `${manual.observedTemperatureC} °C`, source: manual.inspectorName || 'Operations' }].filter(Boolean);
  return <section className="evidence-comparison" aria-label="Sensor and manual evidence">
    <header><h3>Sensor and manual evidence</h3>{contradictory && <strong className="contradiction-line">Contradictory evidence</strong>}</header>
    {rows.length === 0 ? <MissingBox label="No evidence" /> : <DataTable><thead><tr><th>Time</th><th>Source</th><th>Reading</th><th>Recorded by</th></tr></thead><tbody>{rows.map((row, index) => <tr key={`${row.kind}-${index}`}><td>{formatTime(row.time)}</td><td>{row.kind}</td><td>{row.value}</td><td>{row.source}</td></tr>)}</tbody></DataTable>}
  </section>;
}
