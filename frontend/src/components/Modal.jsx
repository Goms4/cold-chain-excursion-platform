import React, { useEffect, useRef } from 'react';

export default function Modal({ title, eyebrow, onClose, children, labelledBy = 'modal-title' }) {
  const dialogRef = useRef(null);
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;
  useEffect(() => {
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const focusable = () => dialogRef.current?.querySelectorAll(
      'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])'
    ) || [];
    focusable()[0]?.focus();
    const onKeyDown = event => {
      if (event.key === 'Escape') { event.stopPropagation(); onCloseRef.current?.(); return; }
      if (event.key !== 'Tab') return;
      const nodes = [...focusable()];
      if (!nodes.length) { event.preventDefault(); dialogRef.current?.focus(); return; }
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus?.();
    };
  }, []);

  return <div className="modal-overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose?.(); }}>
    <section className="modal-box business-modal" role="dialog" aria-modal="true" aria-labelledby={labelledBy} ref={dialogRef} tabIndex={-1}>
      <div className="modal-header"><div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h2 id={labelledBy}>{title}</h2></div><button type="button" className="modal-close" onClick={onClose} aria-label="Close dialog">×</button></div>
      <div className="modal-body">{children}</div>
    </section>
  </div>;
}
