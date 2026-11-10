import React from 'react';

export default function StatusBadge({ status }) {
  if (!status) return null;
  const s = String(status).toUpperCase();

  let styles = 'bg-slate-100 text-slate-700 border-slate-200';

  if (['NORMAL', 'VALIDATED', 'RELEASED', 'COMPLETED', 'DELIVERED', 'ONLINE', 'UP'].includes(s)) {
    styles = 'bg-emerald-50 text-emerald-800 border-emerald-200';
  } else if (['MINOR', 'UNDER_INVESTIGATION', 'IN_TRANSIT', 'IN_REVIEW', 'HOLD', 'PENDING'].includes(s)) {
    styles = 'bg-amber-50 text-amber-800 border-amber-200';
  } else if (['MAJOR', 'EXCURSION', 'DUPLICATE', 'HIGH'].includes(s)) {
    styles = 'bg-orange-50 text-orange-800 border-orange-200';
  } else if (['CRITICAL', 'REJECTED', 'QUARANTINED', 'CONTRADICTORY_EVIDENCE'].includes(s)) {
    styles = 'bg-rose-50 text-rose-800 border-rose-200 font-semibold';
  } else if (['UNKNOWN'].includes(s)) {
    styles = 'bg-purple-50 text-purple-800 border-purple-200 font-mono';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium border ${styles}`}>
      {s}
    </span>
  );
}
