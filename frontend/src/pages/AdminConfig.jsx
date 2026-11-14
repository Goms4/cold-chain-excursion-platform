import React from 'react';

const profiles = [
  {
    id: 'PROFILE-CRT',
    name: 'Controlled room / ambient',
    target: '15.0–25.0°C',
    minor: '25.1–30.0°C (up to 240 min)',
    major: '30.1–40.0°C or minor > 240 min',
    critical: '> 40.0°C or freeze (< 10°C)',
    version: 'v1.0.0',
  },
  {
    id: 'PROFILE-COOL',
    name: 'Cool',
    target: '8.0–15.0°C',
    minor: '15.1–20.0°C (up to 180 min)',
    major: '20.1–25.0°C',
    critical: '> 25.0°C or freeze (< 3°C)',
    version: 'v1.0.0',
  },
  {
    id: 'PROFILE-REF',
    name: 'Refrigerated',
    target: '2.0–8.0°C',
    minor: '8.1–12.0°C (up to 120 min)',
    major: '12.1–20.0°C',
    critical: '> 20.0°C or freeze (< 0°C)',
    version: 'v1.0.0',
  },
  {
    id: 'PROFILE-FRZ',
    name: 'Frozen',
    target: '−25.0 to −10.0°C',
    minor: '−9.9 to −5.0°C (up to 60 min)',
    major: '−4.9 to 0.0°C',
    critical: '> 0.0°C (thaw condition)',
    version: 'v1.0.0',
  },
];

function Row({ label, value }) {
  return (
    <tr style={{ borderBottom: '1px solid var(--border)' }}>
      <td style={{ padding: '8px 16px', color: 'var(--text-muted)', fontSize: 13, width: '45%' }}>{label}</td>
      <td style={{ padding: '8px 16px', fontSize: 13, fontFamily: 'ui-monospace, monospace' }}>{value}</td>
    </tr>
  );
}

export default function AdminConfig() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
      <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text)' }}>Configuration</h1>

      {/* Storage profiles table */}
      <div>
        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 10, color: 'var(--text)' }}>
          Storage profiles
        </div>
        <div className="panel" style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Profile</th>
                <th>Name</th>
                <th>Target range</th>
                <th>Minor excursion</th>
                <th>Major excursion</th>
                <th>Critical breach</th>
                <th>Rule version</th>
              </tr>
            </thead>
            <tbody>
              {profiles.map(p => (
                <tr key={p.id}>
                  <td><span className="mono" style={{ fontSize: 12 }}>{p.id}</span></td>
                  <td style={{ fontSize: 13 }}>{p.name}</td>
                  <td><span className="mono" style={{ fontSize: 12 }}>{p.target}</span></td>
                  <td style={{ fontSize: 12, color: 'var(--sev-minor)' }}>{p.minor}</td>
                  <td style={{ fontSize: 12, color: 'var(--sev-major)' }}>{p.major}</td>
                  <td style={{ fontSize: 12, color: 'var(--sev-critical)' }}>{p.critical}</td>
                  <td><span className="mono" style={{ fontSize: 12 }}>{p.version}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Thresholds */}
      <div>
        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 10, color: 'var(--text)' }}>Thresholds</div>
        <div className="panel">
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <Row label="Authoritative clock" value="event_time (sensor measured)" />
              <Row label="Ingestion clock" value="ingestion_time (retained separately)" />
            </tbody>
          </table>
        </div>
      </div>

      {/* Sampling */}
      <div>
        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 10, color: 'var(--text)' }}>Sampling</div>
        <div className="panel">
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <Row label="Default cadence" value="5 min (gaps > 6 min → Unknown)" />
              <Row label="Duplicate policy" value="Retained in raw, suppressed from incidents" />
            </tbody>
          </table>
        </div>
      </div>

      {/* Late-event window */}
      <div>
        <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 10, color: 'var(--text)' }}>Late-event window</div>
        <div className="panel">
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <Row label="Allowed lateness watermark" value="120 min (quarantined if exceeded)" />
              <Row label="Security model" value="Eight-hour session — OPERATIONS, ANALYST" />
              <Row label="Audit trail" value="Strictly append-only" />
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
