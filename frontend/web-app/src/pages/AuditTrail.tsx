import React, { useEffect, useState, useCallback } from 'react';
import { ShieldCheck, RefreshCw, ChevronDown, ChevronRight, Search } from 'lucide-react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { AuditLogEntry, AuditLogRow, AuditServiceKey } from '../types/audit';
import toast from 'react-hot-toast';

const ENTITY_TYPES: Record<AuditServiceKey, string[]> = {
  inventory: ['Part', 'NCR', 'MaterialReceipt'],
  procurement: ['PurchaseOrder', 'PurchaseRequisition'],
};

const actionColor = (action: string) => {
  if (action.includes('REJECT')) return 'bg-rose-100 text-rose-700';
  if (action.includes('APPROVE')) return 'bg-emerald-100 text-emerald-700';
  if (action === 'CREATE') return 'bg-indigo-100 text-indigo-700';
  if (action === 'UPDATE') return 'bg-amber-100 text-amber-700';
  return 'bg-slate-100 text-slate-700';
};

const AuditTrail: React.FC = () => {
  const [rows, setRows] = useState<AuditLogRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [serviceFilter, setServiceFilter] = useState<'all' | AuditServiceKey>('all');
  const [entityTypeFilter, setEntityTypeFilter] = useState<string>('all');
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState<number | null>(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [invRes, procRes] = await Promise.allSettled([
        api.get<AuditLogEntry[]>('http://localhost:8081/api/audit-log'),
        procurementApi.get<AuditLogEntry[]>('/audit-log'),
      ]);

      const invRows: AuditLogRow[] =
        invRes.status === 'fulfilled' ? invRes.value.data.map(r => ({ ...r, service: 'inventory' as const })) : [];
      const procRows: AuditLogRow[] =
        procRes.status === 'fulfilled' ? procRes.value.data.map(r => ({ ...r, service: 'procurement' as const })) : [];

      if (invRes.status === 'rejected') toast.error('Inventory audit log unavailable');
      if (procRes.status === 'rejected') toast.error('Procurement audit log unavailable');

      setRows([...invRows, ...procRows].sort((a, b) => b.timestamp.localeCompare(a.timestamp)));
    } catch {
      toast.error('Failed to load audit trail');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const filtered = rows.filter(r => {
    if (serviceFilter !== 'all' && r.service !== serviceFilter) return false;
    if (entityTypeFilter !== 'all' && r.entityType !== entityTypeFilter) return false;
    if (search && !`${r.actor} ${r.entityType} ${r.entityId} ${r.action}`.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const availableEntityTypes = serviceFilter === 'all'
    ? [...ENTITY_TYPES.inventory, ...ENTITY_TYPES.procurement]
    : ENTITY_TYPES[serviceFilter];

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black flex items-center gap-2"><ShieldCheck className="text-indigo-600" /> Audit Trail</h1>
          <p className="text-sm text-slate-500">Who did what, when — every CREATE/UPDATE/APPROVE/REJECT across Inventory & Procurement</p>
        </div>
        <button onClick={fetchAll} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 text-white text-sm font-bold hover:bg-indigo-700">
          <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Refresh
        </button>
      </div>

      <div className="flex flex-wrap gap-3">
        <select value={serviceFilter} onChange={e => { setServiceFilter(e.target.value as any); setEntityTypeFilter('all'); }}
          className="px-3 py-2 rounded-lg border border-slate-200 text-sm">
          <option value="all">All Services</option>
          <option value="inventory">Inventory</option>
          <option value="procurement">Procurement</option>
        </select>
        <select value={entityTypeFilter} onChange={e => setEntityTypeFilter(e.target.value)}
          className="px-3 py-2 rounded-lg border border-slate-200 text-sm">
          <option value="all">All Entity Types</option>
          {availableEntityTypes.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <div className="relative flex-1 min-w-[200px]">
          <Search size={16} className="absolute left-3 top-2.5 text-slate-400" />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search actor, entity, action..."
            className="w-full pl-9 pr-3 py-2 rounded-lg border border-slate-200 text-sm" />
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr className="text-xs uppercase tracking-wider text-slate-500">
              <th className="text-left px-4 py-3 w-8"></th>
              <th className="text-left px-4 py-3">Timestamp</th>
              <th className="text-left px-4 py-3">Service</th>
              <th className="text-left px-4 py-3">Entity</th>
              <th className="text-left px-4 py-3">Action</th>
              <th className="text-left px-4 py-3">Actor</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {filtered.map(row => (
              <React.Fragment key={`${row.service}-${row.id}`}>
                <tr className="hover:bg-slate-50/50 cursor-pointer" onClick={() => setExpanded(expanded === row.id ? null : row.id)}>
                  <td className="px-4 py-3">{expanded === row.id ? <ChevronDown size={14} /> : <ChevronRight size={14} />}</td>
                  <td className="px-4 py-3 text-slate-600">{new Date(row.timestamp).toLocaleString()}</td>
                  <td className="px-4 py-3 capitalize">{row.service}</td>
                  <td className="px-4 py-3 font-semibold">{row.entityType} #{row.entityId}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2.5 py-1 text-[11px] font-black uppercase rounded-full ${actionColor(row.action)}`}>{row.action}</span>
                  </td>
                  <td className="px-4 py-3">{row.actor}</td>
                </tr>
                {expanded === row.id && (
                  <tr>
                    <td colSpan={6} className="px-4 py-4 bg-slate-50">
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <div className="text-xs font-bold text-slate-500 mb-1">PREVIOUS VALUE</div>
                          <pre className="text-xs bg-white border border-slate-200 rounded-lg p-3 overflow-auto max-h-64">{row.previousValue ? JSON.stringify(JSON.parse(row.previousValue), null, 2) : '—'}</pre>
                        </div>
                        <div>
                          <div className="text-xs font-bold text-slate-500 mb-1">NEW VALUE</div>
                          <pre className="text-xs bg-white border border-slate-200 rounded-lg p-3 overflow-auto max-h-64">{row.newValue ? JSON.stringify(JSON.parse(row.newValue), null, 2) : '—'}</pre>
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
            {!loading && filtered.length === 0 && (
              <tr><td colSpan={6} className="text-center py-10 text-slate-400">No audit records match these filters.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AuditTrail;
