import React, { useState, useEffect } from 'react';
import { Database, Ban, Copy, History, Loader2 } from 'lucide-react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { Part } from '../types/inventory';
import { Supplier } from '../types/procurement';

interface AuditLogEntry {
  id: number;
  entityType: string;
  entityId: number | null;
  action: string;
  actor: string;
  ipAddress: string | null;
  previousValue: string | null;
  newValue: string | null;
  timestamp: string;
}

type Tab = 'INACTIVE' | 'DUPLICATE' | 'HISTORY';

const MasterDataReports = () => {
  const [tab, setTab] = useState<Tab>('INACTIVE');
  const [loading, setLoading] = useState(false);

  const [inactiveParts, setInactiveParts] = useState<Part[]>([]);
  const [inactiveSuppliers, setInactiveSuppliers] = useState<Supplier[]>([]);
  const [duplicateParts, setDuplicateParts] = useState<Part[]>([]);
  const [duplicateSuppliers, setDuplicateSuppliers] = useState<Supplier[]>([]);
  const [auditLog, setAuditLog] = useState<AuditLogEntry[]>([]);

  const fetchInactive = async () => {
    setLoading(true);
    try {
      const [partsRes, suppliersRes] = await Promise.allSettled([
        api.get('/inventory/reports/inactive-parts'),
        procurementApi.get('/suppliers/reports/inactive'),
      ]);
      if (partsRes.status === 'fulfilled') setInactiveParts(partsRes.value.data.data);
      if (suppliersRes.status === 'fulfilled') setInactiveSuppliers(suppliersRes.value.data);
    } finally {
      setLoading(false);
    }
  };

  const fetchDuplicates = async () => {
    setLoading(true);
    try {
      const [partsRes, suppliersRes] = await Promise.allSettled([
        api.get('/inventory/reports/duplicate-parts'),
        procurementApi.get('/suppliers/reports/duplicates'),
      ]);
      if (partsRes.status === 'fulfilled') setDuplicateParts(partsRes.value.data.data);
      if (suppliersRes.status === 'fulfilled') setDuplicateSuppliers(suppliersRes.value.data);
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const [invRes, procRes] = await Promise.allSettled([
        api.get('/inventory/audit-log'),
        procurementApi.get('/audit-log'),
      ]);
      const combined: AuditLogEntry[] = [
        ...(invRes.status === 'fulfilled' ? invRes.value.data : []),
        ...(procRes.status === 'fulfilled' ? procRes.value.data : []),
      ].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      setAuditLog(combined);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tab === 'INACTIVE') fetchInactive();
    if (tab === 'DUPLICATE') fetchDuplicates();
    if (tab === 'HISTORY') fetchHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  const tabs: { key: Tab; label: string; icon: React.ReactNode }[] = [
    { key: 'INACTIVE', label: 'Inactive Records', icon: <Ban size={14} /> },
    { key: 'DUPLICATE', label: 'Duplicate Records', icon: <Copy size={14} /> },
    { key: 'HISTORY', label: 'Change History', icon: <History size={14} /> },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="p-2.5 bg-slate-800 rounded-2xl text-white">
          <Database size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Master Data Reports</h1>
          <p className="text-xs text-slate-500 font-medium">Inactive records, duplicates, and change history across Parts and Suppliers</p>
        </div>
      </div>

      <div className="flex gap-2 border-b border-slate-200">
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`flex items-center gap-1.5 px-4 py-2.5 text-sm font-bold border-b-2 transition-all ${
              tab === t.key ? 'border-slate-900 text-slate-900' : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}>
            {t.icon} {t.label}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="p-16 text-center"><Loader2 className="animate-spin text-slate-400 mx-auto" size={28} /></div>
      ) : tab === 'INACTIVE' ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-100 font-black text-sm text-slate-700">Inactive Parts ({inactiveParts.length})</div>
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-slate-100">
                {inactiveParts.map(p => (
                  <tr key={p.id}><td className="px-5 py-2.5 font-bold text-slate-800">{p.name}</td><td className="px-5 py-2.5 font-mono text-xs text-slate-500">{p.serialNumber}</td></tr>
                ))}
                {inactiveParts.length === 0 && <tr><td className="px-5 py-6 text-center text-slate-400 text-xs">None</td></tr>}
              </tbody>
            </table>
            </div>
          </div>
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-100 font-black text-sm text-slate-700">Inactive Suppliers ({inactiveSuppliers.length})</div>
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-slate-100">
                {inactiveSuppliers.map(s => (
                  <tr key={s.id}><td className="px-5 py-2.5 font-bold text-slate-800">{s.name}</td><td className="px-5 py-2.5 font-mono text-xs text-slate-500">{s.vendorCode}</td></tr>
                ))}
                {inactiveSuppliers.length === 0 && <tr><td className="px-5 py-6 text-center text-slate-400 text-xs">None</td></tr>}
              </tbody>
            </table>
            </div>
          </div>
        </div>
      ) : tab === 'DUPLICATE' ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-100 font-black text-sm text-slate-700">Duplicate-Named Parts ({duplicateParts.length})</div>
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-slate-100">
                {duplicateParts.map(p => (
                  <tr key={p.id}><td className="px-5 py-2.5 font-bold text-slate-800">{p.name}</td><td className="px-5 py-2.5 font-mono text-xs text-slate-500">{p.serialNumber}</td></tr>
                ))}
                {duplicateParts.length === 0 && <tr><td className="px-5 py-6 text-center text-slate-400 text-xs">None found</td></tr>}
              </tbody>
            </table>
            </div>
          </div>
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-100 font-black text-sm text-slate-700">Duplicate-Named Suppliers ({duplicateSuppliers.length})</div>
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-slate-100">
                {duplicateSuppliers.map(s => (
                  <tr key={s.id}><td className="px-5 py-2.5 font-bold text-slate-800">{s.name}</td><td className="px-5 py-2.5 font-mono text-xs text-slate-500">{s.vendorCode}</td></tr>
                ))}
                {duplicateSuppliers.length === 0 && <tr><td className="px-5 py-6 text-center text-slate-400 text-xs">None found</td></tr>}
              </tbody>
            </table>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
          <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-[10px] uppercase tracking-widest font-black text-slate-400">
              <tr>
                <th className="px-5 py-3">Timestamp</th>
                <th className="px-5 py-3">Entity</th>
                <th className="px-5 py-3">Action</th>
                <th className="px-5 py-3">Actor</th>
                <th className="px-5 py-3">IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLog.map(e => (
                <tr key={`${e.entityType}-${e.id}`}>
                  <td className="px-5 py-2.5 text-xs text-slate-500">{new Date(e.timestamp).toLocaleString()}</td>
                  <td className="px-5 py-2.5 font-mono text-xs font-bold text-slate-700">{e.entityType} #{e.entityId ?? '—'}</td>
                  <td className="px-5 py-2.5 text-xs font-black text-slate-600">{e.action}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-600">{e.actor}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-400">{e.ipAddress ?? '—'}</td>
                </tr>
              ))}
              {auditLog.length === 0 && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-400 text-xs">No audit entries yet</td></tr>}
            </tbody>
          </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default MasterDataReports;