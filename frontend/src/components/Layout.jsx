import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import { PageContainer } from './EditorialUI';

const opsNav = [
  { to: '/dashboard', label: 'Overview', end: true },
  { to: '/shipments', label: 'Shipments' },
  { to: '/issues', label: 'Temperature issues' },
  { to: '/investigations', label: 'Investigations' },
  { to: '/audit', label: 'Activity history' },
  { to: '/configuration', label: 'Configuration' },
];
const analystNav = [
  { to: '/dashboard', label: 'Overview', end: true },
  { to: '/shipments', label: 'Shipments' },
  { to: '/issues', label: 'Temperature issues' },
  { to: '/investigations', label: 'Investigations' },
  { to: '/audit', label: 'Audit history' },
  { to: '/configuration', label: 'Configuration' },
];

export default function Layout({ role, onSignOut }) {
  const nav = role === 'ANALYST' ? analystNav : opsNav;
  return (
    <div className="app-shell">
      <Navbar role={role} onSignOut={onSignOut} />
      <div className="workspace-frame">
        <aside className="sidebar-desktop">
          <nav className="side-nav">
            <div className="sidebar-label">OPERATIONS</div>
            {nav.slice(0, 3).map(({ to, label, end }) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `side-link ${isActive ? 'active' : ''}`}>{label}</NavLink>)}
            <div className="sidebar-label">QUALITY</div>
            {nav.slice(3, 5).map(({ to, label, end }) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `side-link ${isActive ? 'active' : ''}`}>{label}</NavLink>)}
            <div className="sidebar-label">SYSTEM</div>
            {nav.slice(5).map(({ to, label, end }) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `side-link ${isActive ? 'active' : ''}`}>{label}</NavLink>)}
          </nav>
          <div className="sidebar-bottom"><span className="status-dot" /> Cold chain monitoring</div>
        </aside>
        <PageContainer><Outlet /></PageContainer>
      </div>
    </div>
  );
}
