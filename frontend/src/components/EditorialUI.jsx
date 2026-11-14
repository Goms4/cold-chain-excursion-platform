import React from 'react';

export function PageContainer({ children, className = '' }) {
  return <main className={`page-content ${className}`.trim()}>{children}</main>;
}

export function PageHeader({ breadcrumb, title, summary, actions, status }) {
  return <header className="page-heading-row">
    <div>{breadcrumb && <div className="editorial-breadcrumb">{breadcrumb}</div>}<h1>{title}</h1>{summary && <p>{summary}</p>}</div>
    <div className="editorial-header-actions">{status}{actions}</div>
  </header>;
}

export function Panel({ breadcrumb, status, title, children, className = '' }) {
  return <section className={`content-card editorial-panel ${className}`.trim()}>
    {(breadcrumb || status) && <div className="panel-top-strip"><span>{breadcrumb}</span><span>{status}</span></div>}
    {title && <div className="section-heading"><h2>{title}</h2></div>}
    {children}
  </section>;
}

export function DataTable({ children, className = '' }) {
  return <div className={`table-scroll ${className}`.trim()}><table className="business-table">{children}</table></div>;
}

export function MasterDetail({ children, className = '' }) {
  return <div className={`investigation-layout ${className}`.trim()}>{children}</div>;
}

export function NumberedSteps({ steps = [] }) {
  return <ol className="numbered-steps">{steps.map((step, index) => <li className={index === steps.length - 1 ? 'final' : ''} key={step.title || index}>
    <span className="numbered-step-index">{index + 1}</span><div><h3>{step.title}</h3>{step.children || step.description}</div>
  </li>)}</ol>;
}

export function MissingBox({ label = 'Not recorded' }) {
  return <div className="missing-box" role="note"><strong>{label}</strong><span>No readings</span></div>;
}

export function FormRow({ label, children, htmlFor, className = '' }) {
  return <label className={`form-field ${className}`.trim()} htmlFor={htmlFor}><span>{label}</span>{children}</label>;
}

export function Button({ children, className = 'btn-secondary', ...props }) {
  return <button className={className} {...props}>{children}</button>;
}

export function Input({ className = 'form-input', ...props }) {
  return <input className={className} {...props} />;
}
