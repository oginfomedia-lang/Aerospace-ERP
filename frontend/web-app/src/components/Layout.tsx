import React, { useState } from 'react';
import { NavLink, useLocation, Outlet } from 'react-router-dom';
import {
  LayoutDashboard, Package, ShieldCheck, Menu, X,
  Plane, ChevronRight, Bell, LogOut, FileMinus,
  Wrench, Factory, ShoppingCart, Users, Truck, ShieldAlert, MapPin, Building2, Receipt, Route
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface Props {
  children?: React.ReactNode;
}

const navItems = [
  {
    path: '/dashboard',
    label: 'Dashboard',
    icon: <LayoutDashboard size={18} />,
    description: 'KPIs & overview',
    roles: []
  },
  {
    path: '/work-orders',
    label: 'Work Orders',
    icon: <Wrench size={18} />,
    description: 'Maintainance task management',
    roles: ['PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/suppliers',
    label: 'Suppliers',
    icon: <Users size={18} />,
    description: 'Vendor management',
    roles: ['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/purchase-orders',
    label: 'Purchase Orders',
    icon: <ShoppingCart size={18} />,
    description: 'Procurement tracking',
    roles: ['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/vendor-invoices',
    label: 'Vendor Invoices',
    icon: <Receipt size={18} />,
    description: 'Three-way match & AP',
    roles: ['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/debit-notes',
    label: 'Debit Notes',
    icon: <FileMinus size={18} />,
    description: 'Vendor returns & AP credit',
    roles: ['PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/locations',
    label: 'Locations',
    icon: <MapPin size={18} />,
    description: 'Storage location master',
    roles: ['STORE_KEEPER', 'PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/material-receipts',
    label: 'Material Receipts',
    icon: <Truck size={18} />,
    description: 'Gate entry → GRN',
    roles: ['STORE_KEEPER', 'PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/inventory',
    label: 'Inventory',
    icon: <Package size={18} />,
    description: 'Parts & stock ledger',
    roles: ['STORE_KEEPER', 'PROCUREMENT_OFFICER', 'PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/qa',
    label: 'QA Inspection',
    icon: <ShieldCheck size={18} />,
    description: 'Compliance decisions',
    roles: ['QA_INSPECTOR', 'MANAGER', 'ADMIN']
  },
  {
    path: '/ncr',
    label: 'NCR',
    icon: <ShieldAlert size={18} />,
    description: 'Non-conformance disposition',
    roles: ['QA_INSPECTOR', 'STORE_KEEPER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/production',
    label: 'Production',
    icon: <Factory size={18} />,
    description: 'Manufacturing order tracking',
    roles: ['PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/routing-templates',
    label: 'Routing Templates',
    icon: <Route size={18} />,
    description: 'Per-part operation sequences',
    roles: ['PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN']
  },
  {
    path: '/notifications',
    label: 'Notifications',
    icon: <Bell size={18} />,
    description: 'Alerts & event feed',
    roles: []
  },
  {
    path: '/org-settings',
    label: 'Org Settings',
    icon: <Building2 size={18} />,
    description: 'Letterhead & PO defaults',
    roles: ['MANAGER', 'ADMIN']
  },
  {
    path: '/approval-matrix',
    label: 'Approval Matrix',
    icon: <ShieldCheck size={18} />,
    description: 'PO approval bands & roles',
    roles: ['MANAGER', 'ADMIN']
  }
];

const Layout = ({ children }: Props) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const { user, hasRole, logout } = useAuth();
  
  const visibleNavItems = navItems.filter(item =>
    !item.roles.length ||
    !user?.roles ||
    user.roles.length === 0 ||
    item.roles.some(role => hasRole(role))
);

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
        <nav className="flex-1 min-h-0 overflow-y-auto no-scrollbar p-4 space-y-1">
          {visibleNavItems.map(item => (
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
          <div className="flex items-center gap-2 px-3 py-2 bg-slate-800 rounded-xl">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="text-[10px] text-slate-400 font-bold">Procurement · :8086</span>
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
          <div className="flex items-center gap-2 text-sm flex-1">
            <span className="text-slate-400 font-medium">ERP</span>
            <ChevronRight size={14} className="text-slate-300" />
            <span className="font-black text-slate-900 capitalize">
              {location.pathname === '/dashboard' && 'Dashboard'}
              {location.pathname === '/inventory' && 'Inventory'}
              {location.pathname === '/qa' && 'QA Inspection'}
              {location.pathname === '/notifications' && 'Notifications'}
              {location.pathname === '/production' && 'Production'}
              {location.pathname === '/routing-templates' && 'Routing Templates'}
              {location.pathname === '/suppliers' && 'Suppliers'}
              {location.pathname === '/purchase-orders' && 'Purchase Orders'}
              {location.pathname === '/locations' && 'Locations'}
              {location.pathname === '/material-receipts' && 'Material Receipts'}
              {location.pathname === '/ncr' && 'Non-Conformance Reports'}
              {location.pathname === '/org-settings' && 'Organization Settings'}
              {location.pathname === '/approval-matrix' && 'Approval Matrix'}
              {location.pathname === '/vendor-invoices' && 'Vendor Invoices'}
              {location.pathname === '/debit-notes' && 'Debit Notes'}
              {location.pathname.includes('/transactions') && 'Transaction History'}
            </span>
          </div>

          <button
            onClick={logout}
            className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-800 rounded-xl transition-colors font-bold text-sm"
          >
            <LogOut size={16} />
            <span className="hidden sm:inline">Logout</span>
          </button>
        </header>

        {/* Page content */}
        <main className="flex-1 p-6 overflow-auto">
          {children || <Outlet />}
        </main>
      </div>
    </div>
  );
};

export default Layout;
