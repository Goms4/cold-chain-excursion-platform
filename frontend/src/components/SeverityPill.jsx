import React from 'react';

export default function SeverityPill({ severity }) {
  if (!severity) return null;
  const sev = String(severity).toUpperCase();

  let color = 'bg-slate-100 text-slate-700 border-slate-300';
  let dot = 'bg-slate-400';

  if (sev === 'NORMAL') {
    color = 'bg-emerald-50 text-emerald-800 border-emerald-300';
    dot = 'bg-emerald-500';
  } else if (sev === 'MINOR') {
    color = 'bg-amber-50 text-amber-800 border-amber-300';
    dot = 'bg-amber-500';
  } else if (sev === 'MAJOR') {
    color = 'bg-orange-50 text-orange-800 border-orange-400';
    dot = 'bg-orange-500';
  } else if (sev === 'CRITICAL') {
    color = 'bg-rose-50 text-rose-800 border-rose-400 font-bold animate-pulse';
    dot = 'bg-rose-600';
  }

  return (
    <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${color}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dot}`}></span>
      <span>{sev}</span>
    </span>
  );
}
