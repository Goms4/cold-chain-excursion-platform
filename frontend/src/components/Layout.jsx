import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Truck, AlertTriangle, FileSearch, History, Settings } from 'lucide-react';
import Navbar from './Navbar';
import ScenarioRunnerBar from './ScenarioRunnerBar';

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/shipments', label: 'Shipments', icon: Truck },
  { to: '/excursions', label: 'Excursions', icon: AlertTriangle },
  { to: '/investigations', label: 'Investigations', icon: FileSearch },
  { to: '/audit', label: 'Audit History', icon: History },
  { to: '/admin', label: 'Configuration', icon: Settings },
];

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-800">
      <Navbar />

      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-60 bg-white border-r border-clinical-border flex flex-col py-4">
          <nav className="flex-1 px-3 space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/'}
                  className={({ isActive }) =>
                    `flex items-center space-x-3 px-3 py-2 rounded-md text-xs font-medium transition-colors ${
                      isActive
                        ? 'bg-slate-100 text-pharma-800 font-semibold border-l-2 border-pharma-700'
                        : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                    }`
                  }
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>

          <div className="px-4 py-3 border-t border-clinical-border text-[11px] text-slate-500 font-mono">
            <div>PostgreSQL 17 • Java 17</div>
            <div className="text-slate-400">Append-Only Audit</div>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 p-6 overflow-y-auto">
          <ScenarioRunnerBar />
          <Outlet />
        </main>
      </div>
    </div>
  );
}
