import React from 'react';
import { Settings, Shield, Sliders, Database } from 'lucide-react';

const profiles = [
  {
    id: 'PROFILE-CRT',
    name: 'Controlled Room / Ambient',
    target: '15.0°C - 25.0°C',
    minor: '25.1°C - 30.0°C (up to 240 mins)',
    major: '30.1°C - 40.0°C (or minor > 240 mins)',
    critical: '> 40.0°C or freezing breach (< 10°C)',
    version: 'v1.0.0'
  },
  {
    id: 'PROFILE-COOL',
    name: 'Cool',
    target: '8.0°C - 15.0°C',
    minor: '15.1°C - 20.0°C (up to 180 mins)',
    major: '20.1°C - 25.0°C',
    critical: '> 25.0°C or freezing breach (< 3°C)',
    version: 'v1.0.0'
  },
  {
    id: 'PROFILE-REF',
    name: 'Refrigerated',
    target: '2.0°C - 8.0°C',
    minor: '8.1°C - 12.0°C (up to 120 mins)',
    major: '12.1°C - 20.0°C',
    critical: '> 20.0°C or freeze condition (< 0°C)',
    version: 'v1.0.0'
  },
  {
    id: 'PROFILE-FRZ',
    name: 'Frozen',
    target: '-25.0°C to -10.0°C',
    minor: '-9.9°C to -5.0°C (up to 60 mins)',
    major: '-4.9°C to 0.0°C',
    critical: '> 0.0°C (Thaw condition)',
    version: 'v1.0.0'
  }
];

export default function AdminConfig() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-bold text-slate-900 tracking-tight">System & Storage Profile Configuration</h2>
        <p className="text-xs text-clinical-muted">Authoritative pharmaceutical storage profiles, telemetry cadence definitions, and watermark thresholds</p>
      </div>

      {/* Storage Profiles Table */}
      <div className="bg-white border border-clinical-border rounded-lg overflow-hidden clinical-card">
        <div className="px-5 py-3.5 border-b border-clinical-border bg-slate-50 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Configured Pharmaceutical Storage Profiles
            </h3>
          </div>
          <span className="text-[11px] font-mono text-slate-500">Flyway Seeded • Versioned</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/50 text-[10px] text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-2.5 px-4">Profile ID</th>
                <th className="py-2.5 px-4">Name</th>
                <th className="py-2.5 px-4 text-emerald-800">Target Safe Band</th>
                <th className="py-2.5 px-4 text-amber-800">Minor Excursion</th>
                <th className="py-2.5 px-4 text-orange-800">Major Excursion</th>
                <th className="py-2.5 px-4 text-rose-800">Critical Breach</th>
                <th className="py-2.5 px-4 text-right">Rule Version</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {profiles.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50/80 transition">
                  <td className="py-3 px-4 font-mono font-semibold text-slate-900">{p.id}</td>
                  <td className="py-3 px-4 font-medium text-slate-800">{p.name}</td>
                  <td className="py-3 px-4 font-mono font-bold text-emerald-700 bg-emerald-50/40">{p.target}</td>
                  <td className="py-3 px-4 font-mono text-amber-800">{p.minor}</td>
                  <td className="py-3 px-4 font-mono text-orange-800">{p.major}</td>
                  <td className="py-3 px-4 font-mono text-rose-700 font-bold">{p.critical}</td>
                  <td className="py-3 px-4 font-mono text-slate-500 text-right">{p.version}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Global Watermark & Telemetry Parameters */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white border border-clinical-border rounded-lg p-5 clinical-card">
          <div className="flex items-center space-x-2 border-b border-clinical-border pb-3 mb-3">
            <Sliders className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Event-Time & Telemetry Rules
            </h3>
          </div>
          <div className="space-y-3 text-xs">
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Authoritative Clock:</span>
              <span className="font-mono font-semibold text-slate-900">event_time (Sensor Measured)</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Platform Ingestion Clock:</span>
              <span className="font-mono text-slate-600">ingestion_time (Retained separately)</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Default Expected Cadence:</span>
              <span className="font-mono font-semibold text-purple-700">5 minutes (Gaps &gt; 6m = UNKNOWN)</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Allowed Lateness Watermark:</span>
              <span className="font-mono font-semibold text-rose-700">120 minutes (Quarantined if breached)</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-600">Duplicate Event Policy:</span>
              <span className="font-mono text-indigo-700">Retained in raw, suppressed from incidents</span>
            </div>
          </div>
        </div>

        <div className="bg-white border border-clinical-border rounded-lg p-5 clinical-card">
          <div className="flex items-center space-x-2 border-b border-clinical-border pb-3 mb-3">
            <Database className="w-4 h-4 text-pharma-700" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Architecture & Stack Constraints
            </h3>
          </div>
          <div className="space-y-3 text-xs">
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Operational Runtime:</span>
              <span className="font-mono font-semibold text-slate-900">Java 17 • Spring Boot Modular Monolith</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Relational Store:</span>
              <span className="font-mono text-slate-700">PostgreSQL 17 (Flyway versioned)</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Analytical Warehouse:</span>
              <span className="font-mono text-slate-700">Snowflake (DIM / FACT model)</span>
            </div>
            <div className="flex justify-between border-b border-slate-100 pb-2">
              <span className="text-slate-600">Security Model:</span>
              <span className="font-mono text-slate-700">Stateless JWT (ADMIN, OPERATIONS, ANALYST)</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-600">Audit Trail:</span>
              <span className="font-mono text-emerald-700 font-semibold">Strictly Append-Only Ledger</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
