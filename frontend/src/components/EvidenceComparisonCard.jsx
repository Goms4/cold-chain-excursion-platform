import React from 'react';
import { AlertCircle, CheckCircle, ShieldAlert, Thermometer, UserCheck } from 'lucide-react';
import StatusBadge from './StatusBadge';

export default function EvidenceComparisonCard({ excursion, manualInspections = [] }) {
  const latestInspection = manualInspections[manualInspections.length - 1];
  const hasContradiction = latestInspection?.contradictsSensor;

  return (
    <div className="bg-white border border-clinical-border rounded-lg p-5 clinical-card mb-6">
      <div className="flex items-center justify-between border-b border-clinical-border pb-3 mb-4">
        <div className="flex items-center space-x-2">
          <Thermometer className="w-4 h-4 text-pharma-700" />
          <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
            Evidence Correlation: Telemetry vs Physical Inspection
          </h3>
        </div>
        {hasContradiction ? (
          <div className="flex items-center space-x-1.5 px-3 py-1 bg-rose-50 text-rose-800 border border-rose-300 rounded text-xs font-semibold animate-pulse">
            <ShieldAlert className="w-3.5 h-3.5" />
            <span>CONTRADICTORY EVIDENCE DETECTED</span>
          </div>
        ) : (
          <span className="text-[11px] font-mono text-slate-500">Dual Independent Evidence Stream</span>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Sensor Evidence Column */}
        <div className="bg-slate-50 border border-slate-200 rounded p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-800 uppercase tracking-wider flex items-center space-x-1.5">
              <span>Automated Sensor Telemetry</span>
            </span>
            <StatusBadge status={excursion ? 'EXCURSION' : 'NORMAL'} />
          </div>

          <div className="space-y-2 mt-3 text-xs">
            <div className="flex justify-between border-b border-slate-200 pb-1">
              <span className="text-slate-500">Peak Observed Temp:</span>
              <span className="font-mono font-bold text-rose-700">{excursion?.peakTemperatureC}°C</span>
            </div>
            <div className="flex justify-between border-b border-slate-200 pb-1">
              <span className="text-slate-500">Severity Assessment:</span>
              <span className="font-semibold text-slate-800">{excursion?.severity}</span>
            </div>
            <div className="flex justify-between border-b border-slate-200 pb-1">
              <span className="text-slate-500">Rule Set Applied:</span>
              <span className="font-mono text-slate-700">{excursion?.ruleVersion}</span>
            </div>
            <div className="flex justify-between border-b border-slate-200 pb-1">
              <span className="text-slate-500">Duration:</span>
              <span className="font-mono text-slate-700">{excursion?.durationMinutes} mins</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Time Range:</span>
              <span className="font-mono text-[11px] text-slate-600">
                {excursion?.startEventTime ? new Date(excursion.startEventTime).toLocaleTimeString() : 'N/A'} - {excursion?.endEventTime ? new Date(excursion.endEventTime).toLocaleTimeString() : 'Present'}
              </span>
            </div>
          </div>
        </div>

        {/* Manual Inspection Column */}
        <div className={`border rounded p-4 ${hasContradiction ? 'bg-amber-50/50 border-amber-200' : 'bg-slate-50 border-slate-200'}`}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-800 uppercase tracking-wider flex items-center space-x-1.5">
              <UserCheck className="w-3.5 h-3.5 text-slate-600" />
              <span>Receiving Manual Inspection</span>
            </span>
            {latestInspection ? (
              <StatusBadge status={latestInspection.contradictsSensor ? 'CONTRADICTORY' : 'CORROBORATED'} />
            ) : (
              <span className="text-[11px] font-mono text-slate-400">PENDING RECEIPT</span>
            )}
          </div>

          {latestInspection ? (
            <div className="space-y-2 mt-3 text-xs">
              <div className="flex justify-between border-b border-slate-200 pb-1">
                <span className="text-slate-500">Inspector Name:</span>
                <span className="font-medium text-slate-800">{latestInspection.inspectorName}</span>
              </div>
              <div className="flex justify-between border-b border-slate-200 pb-1">
                <span className="text-slate-500">Dock Measured Temp:</span>
                <span className={`font-mono font-bold ${hasContradiction ? 'text-emerald-700' : 'text-slate-800'}`}>
                  {latestInspection.observedTemperatureC}°C
                </span>
              </div>
              <div className="flex justify-between border-b border-slate-200 pb-1">
                <span className="text-slate-500">Package Integrity:</span>
                <span className="font-medium text-slate-700">{latestInspection.packageIntegrityStatus}</span>
              </div>
              <div className="border-b border-slate-200 pb-1">
                <span className="text-slate-500">Condition Notes:</span>
                <p className="mt-0.5 text-slate-700 italic text-[11px]">{latestInspection.visualCondition || 'No visual anomalies noted.'}</p>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Inspection Time:</span>
                <span className="font-mono text-[11px] text-slate-600">
                  {new Date(latestInspection.inspectionTime).toLocaleTimeString()}
                </span>
              </div>
            </div>
          ) : (
            <div className="py-8 text-center text-slate-400 text-xs">
              No manual dock inspection has been registered for this incident.
            </div>
          )}
        </div>
      </div>

      {hasContradiction && (
        <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded text-xs text-amber-900 leading-relaxed">
          <strong>Pharmaceutical Governance Notice:</strong> Sensor indicates an excursion ({excursion?.peakTemperatureC}°C) while receiving dock inspection measured compliant temperature ({latestInspection?.observedTemperatureC}°C). Both pieces of evidence remain permanently recorded. An authorized Analyst or Admin must review product stability criteria and log a reasoned disposition decision.
        </div>
      )}
    </div>
  );
}
