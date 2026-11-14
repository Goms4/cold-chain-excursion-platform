import React from 'react';

/**
 * SeverityMarker — 8px square marker + text. Replaces SeverityPill.
 * Never a pill, never a dot, never coloured backgrounds.
 */
export default function SeverityMarker({ severity, className = '' }) {
  if (!severity) return null;
  const s = String(severity).toUpperCase();

  let color = 'var(--sev-minor)';
  if (s === 'CRITICAL') color = 'var(--sev-critical)';
  else if (s === 'MAJOR') color = 'var(--sev-major)';
  else if (s === 'MINOR') color = 'var(--sev-minor)';
  else if (s === 'NORMAL') color = 'var(--sev-minor)';

  const label = s.charAt(0) + s.slice(1).toLowerCase();

  return (
    <span className={`inline-flex items-center gap-1.5 ${className}`} style={{ color }}>
      <span
        className="sev-marker"
        style={{ background: color }}
        aria-hidden="true"
      />
      {label}
    </span>
  );
}
