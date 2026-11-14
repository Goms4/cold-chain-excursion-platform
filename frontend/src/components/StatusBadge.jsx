import React from 'react';

/**
 * StatusBadge — neutral outlined tag. Used sparingly where a status label
 * must stand out as a tag. NOT a coloured pill.
 */
export default function StatusBadge({ status }) {
  if (!status) return null;
  const s = String(status);
  // Humanise: underscores → spaces, sentence case
  const label = s.replace(/_/g, ' ').charAt(0).toUpperCase() + s.replace(/_/g, ' ').slice(1).toLowerCase();
  return (
    <span className="status-tag">{label}</span>
  );
}
