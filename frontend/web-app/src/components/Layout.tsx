import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Package, ShieldCheck, Menu, X,
  Plane, ChevronRight, Bell,
  Wrench, Factory
} from 'lucide-react';
import path from 'path';

interface Props {
  children: React.ReactNode;
}

const navItems = [
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: <LayoutDashboard size={18} />,
    description: 'KPIs & overview'
  },
  {
    path: '/inventory',
    label: 'Inventory',
    icon: <Package size={18} />,
    description: 'Parts & stock ledger'
  },
  {
    path: '/qa',
    label: 'QA Inspection',
    icon: <ShieldCheck size={18} />,
    description: 'Compliance decisions'   // talks to qa-service port 8082
  },
  {
    path: '/notifications',
    label: 'Notifications',
    icon: <Bell size={18} />,
    description: 'Alerts & event feed'    // talks to notification-service port 8083
  },
  {
    path: '/work-orders',
    label: 'Work Orders',
    icon: <Wrench size={18} />,
    description: 'Maintainance task management'
  },
  {
    path: '/production',
    label: 'Production',
    icon: <Factory size={18} />,
    description: 'Manufacturing order tracking'
  }
];

const Layout = ({ children }: Props) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  return (
    <div className="min-h-screen bg-slate-50 flex">

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-slate-900/50 z-20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`
        fixed top-0 left-0 h-full w-64 bg-slate-900 z-30 flex flex-col
        transform transition-transform duration-300 ease-in-out
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
        lg:translate-x-0 lg:static lg:z-auto
      `}>

        {/* Logo */}
        <div className="p-6 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600 rounded-xl">
              <Plane size={20} className="text-white" />
            </div>
            <div>
              <div className="font-black text-white text-sm tracking-tight">AEROSPACE ERP</div>
              <div className="text-[10px] text-slate-500 font-bold uppercase tracking-widest">
                Inventory & QA
              </div>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-1">
          {navItems.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => `
                flex items-center gap-3 px-4 py-3 rounded-2xl transition-all group
                ${isActive
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/50'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                }
              `}
            >
              <span className="shrink-0">{item.icon}</span>
              <div className="flex-1 min-w-0">
                <div className="font-bold text-sm">{item.label}</div>
                <div className="text-[10px] opacity-60 font-medium truncate">{item.description}</div>
              </div>
              <ChevronRight size={14} className="opacity-30 group-hover:opacity-60 transition-opacity" />
            </NavLink>
          ))}
        </nav>

        {/* Service status indicator */}
        <div className="p-4 border-t border-slate-800 space-y-2">
          <div className="text-[10px] text-slate-600 font-black uppercase tracking-widest mb-3">
            Services
          </div>
          <div className="flex items-center gap-2 px-3 py-2 bg-slate-800 rounded-xl">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] text-slate-400 font-bold">Inventory · :8081</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-2 bg-slate-800 rounded-xl">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] text-slate-400 font-bold">QA Service · :8082</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-2 bg-slate-800 rounded-xl">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] text-slate-400 font-bold">Notifications · :8083</span>
          </div>
          <div className="flex items-center gap-2 px-3 py-2 bg-slate-800 rounded-xl">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] text-slate-400 font-bold">Production · :8085</span>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">

        {/* Top bar */}
        <header className="bg-white border-b border-slate-100 px-6 py-4 flex items-center gap-4 sticky top-0 z-10">
          <button
            onClick={() => setSidebarOpen(true)}
            className="lg:hidden p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors"
          >
            {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
          </button>

          {/* Breadcrumb */}
          <div className="flex items-center gap-2 text-sm">
            <span className="text-slate-400 font-medium">ERP</span>
            <ChevronRight size={14} className="text-slate-300" />
            <span className="font-black text-slate-900 capitalize">
              {location.pathname === '/dashboard' && 'Dashboard'}
              {location.pathname === '/inventory' && 'Inventory'}
              {location.pathname === '/qa' && 'QA Inspection'}
              {location.pathname === '/notifications' && 'Notifications'}
              {location.pathname === '/production' && 'Production'}
              {location.pathname.includes('/transactions') && 'Transaction History'}
            </span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-6 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
