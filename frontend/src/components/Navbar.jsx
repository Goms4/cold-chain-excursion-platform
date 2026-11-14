import React from 'react';
import { LogOut } from 'lucide-react';
import { getActiveDisplayName } from '../api/client';

export default function Navbar({ role, onSignOut }) {
  return (
    <header className="topbar">
      <div className="topbar-brand">
        <span className="brand-name">ColdLatch</span>
      </div>
      <div className="topbar-actions">
        <span className="role-chip">{getActiveDisplayName() || (role === 'ANALYST' ? 'Analyst' : 'Operations')}</span>
        <button className="signout-button" onClick={onSignOut}><LogOut size={15} /><span>Sign out</span></button>
      </div>
    </header>
  );
}
