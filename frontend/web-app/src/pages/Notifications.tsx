import React, { useEffect, useState, useCallback, useRef } from 'react';
import {
  Bell, BellOff, Wifi, WifiOff, RefreshCw, Filter,
  AlertTriangle, CheckCircle2, XCircle, Loader2, Mail, Radio, Info,
  Inbox,
} from 'lucide-react';
import notificationApi from '../api/notificationAxios';
import { useNotificationSocket } from '../hooks/useNotificationSocket';
import { NotificationLog, NotificationType, NotificationStats } from '../types/notification';
import { typeConfig, TYPE_GROUPS } from '../utils/notificationConfig';
import { useAuth } from '../context/AuthContext';

// ── Style maps ────────────────────────────────────────────────────────────────

const severityStyle: Record<string, { badge: string; dot: string }> = {
  SUCCESS: { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' },
  WARNING: { badge: 'bg-amber-50  text-amber-700  border-amber-200',     dot: 'bg-amber-500'  },
  ERROR:   { badge: 'bg-red-50    text-red-700    border-red-200',        dot: 'bg-red-500'    },
  INFO:    { badge: 'bg-blue-50   text-blue-700   border-blue-200',       dot: 'bg-blue-500'   },
};

// ── Filter tabs ───────────────────────────────────────────────────────────────

type FilterOption = 'ALL' | 'WARNING' | 'ERROR' | 'SUCCESS' | 'INFO' | 'FAILED';

const FILTERS: { key: FilterOption; label: string }[] = [
  { key: 'ALL',     label: 'All'      },
  { key: 'WARNING', label: 'Warnings' },
  { key: 'ERROR',   label: 'Errors'   },
  { key: 'SUCCESS', label: 'Success'  },
  { key: 'INFO',    label: 'Info'     },
  { key: 'FAILED',  label: 'Failed'   },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function relativeTime(iso: string): string {
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (diff < 60)    return `${diff}s ago`;
  if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return new Date(iso).toLocaleDateString();
}

function computeStats(logs: NotificationLog[]): NotificationStats {
  return {
    total:    logs.length,
    warnings: logs.filter(l => l.severity === 'WARNING').length,
    errors:   logs.filter(l => l.severity === 'ERROR').length,
    success:  logs.filter(l => l.severity === 'SUCCESS').length,
    info:     logs.filter(l => l.severity === 'INFO').length,
    failed:   logs.filter(l => l.status === 'FAILED').length,
  };
}

// ── NotificationCard ──────────────────────────────────────────────────────────

const NotificationCard: React.FC<{ notification: NotificationLog; isNew?: boolean }> = ({
  notification: n, isNew,
}) => {
  const cfg = typeConfig[n.type] ?? {
    icon: <Info size={16} />, iconBg: 'bg-slate-100 text-slate-600', label: n.type,
  };
  const sev = severityStyle[n.severity] ?? severityStyle['INFO'];

  // For work order notifications partName holds the WO number, batchNumber holds aircraftId
  const isWoType = n.type.startsWith('WORK_ORDER');
  const displayName  = isWoType ? n.partName  : n.partName;   // WO number or part name
  const displayBatch = isWoType ? n.batchNumber : n.batchNumber; // aircraft ID or batch

  return (
    <div className={`group p-5 border-b border-slate-50 hover:bg-slate-50/60 transition-all duration-200
      ${isNew ? 'animate-pulse bg-blue-50/40' : ''}`}>
      <div className="flex items-start gap-4">
        <div className={`flex-shrink-0 w-9 h-9 rounded-2xl flex items-center justify-center mt-0.5 ${cfg.iconBg}`}>
          {cfg.icon}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="font-black text-slate-900 text-sm">{displayName ?? '—'}</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-black border ${sev.badge}`}>
              {n.severity}
            </span>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-500 border border-slate-200">
              {cfg.label}
            </span>
          </div>

          <p className="text-xs text-slate-500 font-medium leading-relaxed line-clamp-2">
            {n.message}
          </p>

          <div className="flex items-center gap-4 mt-2 flex-wrap">
            {displayBatch && (
              <span className={`font-mono text-[10px] font-bold px-2 py-0.5 rounded border ${
                isWoType
                  ? 'text-slate-600 bg-slate-100 border-slate-200'
                  : 'text-blue-600 bg-blue-50 border-blue-100'
              }`}>
                {isWoType ? `✈ ${displayBatch}` : displayBatch}
              </span>
            )}
            {n.location && (
              <span className="text-[10px] text-slate-400 font-bold">📍 {n.location}</span>
            )}
            {n.inspectorName && (
              <span className="text-[10px] text-slate-400 font-bold">👤 {n.inspectorName}</span>
            )}
            <div className="flex items-center gap-2 ml-auto">
              {n.websocketSent && (
                <span className="inline-flex items-center gap-1 text-[10px] text-emerald-600 font-bold">
                  <Radio size={10} /> WS
                </span>
              )}
              {n.emailSent && (
                <span className="inline-flex items-center gap-1 text-[10px] text-blue-600 font-bold">
                  <Mail size={10} /> Email
                </span>
              )}
              {n.status === 'FAILED' && (
                <span className="inline-flex items-center gap-1 text-[10px] text-red-500 font-bold">
                  <XCircle size={10} /> Failed
                </span>
              )}
              <span className="text-[10px] text-slate-400 font-bold">{relativeTime(n.createdAt)}</span>
            </div>
          </div>

          {n.errorMessage && (
            <div className="mt-2 text-[10px] text-red-500 font-bold bg-red-50 px-2 py-1 rounded-lg border border-red-100">
              ⚠ {n.errorMessage}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ── Stat card ─────────────────────────────────────────────────────────────────

const StatCard: React.FC<{
  title: string; value: number; icon: React.ReactNode; color: string; note: string;
}> = ({ title, value, icon, color, note }) => (
  <div className="group bg-white rounded-3xl p-6 shadow-sm border border-slate-100 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
    <div className={`p-4 rounded-2xl bg-gradient-to-br ${color} text-white shadow-lg w-fit`}>{icon}</div>
    <div className="mt-6">
      <p className="text-[11px] font-black text-slate-400 uppercase tracking-[0.15em] mb-1">{title}</p>
      <p className="text-3xl font-black text-slate-900 tracking-tight">{value}</p>
      <p className="text-[10px] text-slate-400 mt-1 font-medium">{note}</p>
    </div>
  </div>
);

// ── Type breakdown sidebar — grouped ──────────────────────────────────────────

const TypeBreakdown: React.FC<{
  logs: NotificationLog[];
  activeType: NotificationType | 'ALL';
  onSelect: (t: NotificationType | 'ALL') => void;
}> = ({ logs, activeType, onSelect }) => {
  const counts = logs.reduce<Record<string, number>>((acc, l) => {
    acc[l.type] = (acc[l.type] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
      <div className="p-5 border-b border-slate-100">
        <h3 className="font-black text-slate-800 text-sm uppercase tracking-wider">By type</h3>
      </div>
      <div className="p-2">
        {/* All button */}
        <button
          onClick={() => onSelect('ALL')}
          className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-bold transition-all mb-2
            ${activeType === 'ALL' ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-50'}`}
        >
          <span>All types</span>
          <span className={`text-[10px] px-2 py-0.5 rounded-full font-black
            ${activeType === 'ALL' ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-500'}`}>
            {logs.length}
          </span>
        </button>

        {/* Grouped types */}
        {TYPE_GROUPS.map(group => (
          <div key={group.label} className="mb-3">
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-3 mb-1">
              {group.label}
            </p>
            {group.types.map(t => {
              const cfg = typeConfig[t];
              const cnt = counts[t] ?? 0;
              const active = activeType === t;
              return (
                <button
                  key={t}
                  onClick={() => onSelect(t)}
                  className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-bold transition-all mb-0.5
                    ${active ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-50'}`}
                >
                  <span className={`w-5 h-5 rounded-lg flex items-center justify-center flex-shrink-0 ${
                    active ? 'bg-white/20' : cfg.iconBg
                  }`}>
                    {React.isValidElement(cfg.icon)
                      ? React.cloneElement(cfg.icon as React.ReactElement<{ size: number }>, { size: 11 })
                      : cfg.icon}
                  </span>
                  <span className="flex-1 text-left">{cfg.label}</span>
                  {cnt > 0 && (
                    <span className={`text-[10px] px-2 py-0.5 rounded-full font-black
                      ${active ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-500'}`}>
                      {cnt}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
};

// ── Main Page ─────────────────────────────────────────────────────────────────

const Notifications: React.FC = () => {
  const { token } = useAuth();
  const [logs, setLogs]               = useState<NotificationLog[]>([]);
  const [loading, setLoading]         = useState(true);
  const [filter, setFilter]           = useState<FilterOption>('ALL');
  const [typeFilter, setTypeFilter]   = useState<NotificationType | 'ALL'>('ALL');
  const [wsConnected, setWsConnected] = useState(false);
  const [newIds, setNewIds]           = useState<Set<number>>(new Set());
  const [partIdSearch, setPartIdSearch] = useState('');
  const [batchSearch, setBatchSearch]   = useState('');
  const feedRef = useRef<HTMLDivElement>(null);

  // ── REST fetch ────────────────────────────────────────────────────────────

  const fetchRecent = useCallback(async () => {
    setLoading(true);
    try {
      const res = await notificationApi.get<{ status: string; data: NotificationLog[] }>(
        '/notifications/recent'
      );
      setLogs(res.data.data);
    } catch { /* interceptor handles toast */ }
    finally { setLoading(false); }
  }, []);

  const fetchByPartId = async () => {
    if (!partIdSearch.trim()) return fetchRecent();
    setLoading(true);
    try {
      const res = await notificationApi.get<{ status: string; data: NotificationLog[] }>(
        `/notifications/part/${partIdSearch.trim()}`
      );
      setLogs(res.data.data);
      setTypeFilter('ALL');
      setFilter('ALL');
    } catch { } finally { setLoading(false); }
  };

  const fetchByBatch = async () => {
    if (!batchSearch.trim()) return fetchRecent();
    setLoading(true);
    try {
      const res = await notificationApi.get<{ status: string; data: NotificationLog[] }>(
        `/notifications/batch/${encodeURIComponent(batchSearch.trim())}`
      );
      setLogs(res.data.data);
      setTypeFilter('ALL');
      setFilter('ALL');
    } catch { } finally { setLoading(false); }
  };

  useEffect(() => { fetchRecent(); }, [fetchRecent]);

  // ── WebSocket ─────────────────────────────────────────────────────────────

  // The on-screen popup toast for a new notification is now shown globally by
  // useGlobalNotificationToasts (wired into Layout.tsx) — this page only needs to
  // keep its own live list in sync, not pop up a second toast on top of that one.
  const handleNewNotification = useCallback((n: NotificationLog) => {
    setLogs(prev => [n, ...prev]);
    setNewIds(prev => new Set(prev).add(n.id));

    setTimeout(() => setNewIds(prev => {
      const s = new Set(prev); s.delete(n.id); return s;
    }), 3000);
  }, []);

  useNotificationSocket({
    token,
    onMessage:    handleNewNotification,
    onConnect:    () => setWsConnected(true),
    onDisconnect: () => setWsConnected(false),
  });

  // ── Filtering ─────────────────────────────────────────────────────────────

  const filtered = logs.filter(l => {
    const sevOk = filter === 'ALL' ? true
      : filter === 'FAILED' ? l.status === 'FAILED'
      : l.severity === filter;
    const typeOk = typeFilter === 'ALL' || l.type === typeFilter;
    return sevOk && typeOk;
  });

  const stats = computeStats(logs);

  const handleTypeSidebarClick = async (t: NotificationType | 'ALL') => {
    setTypeFilter(t);
    if (t !== 'ALL') setFilter('ALL');
    if (t === 'ALL') { fetchRecent(); return; }
    setLoading(true);
    try {
      const res = await notificationApi.get<{ status: string; data: NotificationLog[] }>(
        `/notifications/type/${t}`
      );
      setLogs(res.data.data);
    } catch { } finally { setLoading(false); }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">

      {/* Header */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-gradient-to-br from-blue-600 to-blue-700 rounded-2xl text-white shadow-lg shadow-blue-200">
              <Bell size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Notification Center</h1>
              <p className="text-slate-500 text-sm font-medium">
                Real-time alerts from Inventory, QA &amp; Work Orders via Kafka + WebSocket.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold border
              ${wsConnected
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-slate-50 text-slate-500 border-slate-200'}`}>
              {wsConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
              {wsConnected ? 'Live' : 'Offline'}
              {wsConnected && <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />}
            </div>
            <button
              onClick={fetchRecent}
              className="p-2 hover:bg-slate-100 rounded-xl transition text-slate-400 hover:text-slate-700"
              title="Refresh"
            >
              <RefreshCw size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {[
          { title: 'Total',    value: stats.total,    icon: <Bell size={18}/>,          color: 'from-blue-600 to-blue-700',      note: 'All notifications'    },
          { title: 'Warnings', value: stats.warnings, icon: <AlertTriangle size={18}/>,  color: 'from-amber-500 to-amber-600',    note: 'Low stock / expiry'   },
          { title: 'Errors',   value: stats.errors,   icon: <XCircle size={18}/>,         color: 'from-red-600 to-red-700',        note: 'Rejected / expired'   },
          { title: 'Success',  value: stats.success,  icon: <CheckCircle2 size={18}/>,    color: 'from-emerald-600 to-emerald-700',note: 'Approved / completed' },
          { title: 'Info',     value: stats.info,     icon: <Info size={18}/>,            color: 'from-indigo-600 to-indigo-700',  note: 'Stock & WO events'    },
          { title: 'Failed',   value: stats.failed,   icon: <BellOff size={18}/>,         color: 'from-slate-700 to-slate-800',   note: 'Dispatch failures'    },
        ].map((c, i) => (
          <StatCard key={i} title={c.title} value={c.value} icon={c.icon} color={c.color} note={c.note} />
        ))}
      </div>

      {/* Search bar */}
      <div className="bg-white rounded-3xl p-5 shadow-sm border border-slate-100">
        <div className="flex items-center gap-3 flex-wrap">
          <Filter size={14} className="text-slate-400 flex-shrink-0" />
          <span className="text-xs font-black text-slate-400 uppercase tracking-widest">Search:</span>
          <div className="flex items-center gap-2">
            <input
              type="number"
              placeholder="Part / WO ID"
              value={partIdSearch}
              onChange={e => setPartIdSearch(e.target.value)}
              className="w-32 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 outline-none font-medium"
            />
            <button
              onClick={fetchByPartId}
              className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-700 transition"
            >
              Fetch
            </button>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Batch / Aircraft ID"
              value={batchSearch}
              onChange={e => setBatchSearch(e.target.value)}
              className="w-44 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 outline-none font-medium font-mono"
            />
            <button
              onClick={fetchByBatch}
              className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-700 transition"
            >
              Fetch
            </button>
          </div>
          <button
            onClick={() => {
              setPartIdSearch('');
              setBatchSearch('');
              setFilter('ALL');
              setTypeFilter('ALL');
              fetchRecent();
            }}
            className="px-4 py-2 text-xs font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Body */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">

        {/* Sidebar */}
        <div>
          <TypeBreakdown logs={logs} activeType={typeFilter} onSelect={handleTypeSidebarClick} />
        </div>

        {/* Feed */}
        <div className="lg:col-span-3 space-y-4">

          {/* Severity tabs */}
          <div className="flex items-center gap-2 flex-wrap">
            {FILTERS.map(f => (
              <button
                key={f.key}
                onClick={() => setFilter(f.key)}
                className={`px-4 py-2 rounded-2xl text-xs font-black transition-all border
                  ${filter === f.key
                    ? 'bg-slate-900 text-white border-slate-900 shadow-lg'
                    : 'bg-white text-slate-500 border-slate-200 hover:border-slate-300'}`}
              >
                {f.label}
                {f.key === 'ALL'    ? ` (${logs.length})`
                : f.key === 'FAILED' ? ` (${stats.failed})`
                : ` (${logs.filter(l => l.severity === f.key).length})`}
              </button>
            ))}
          </div>

          {/* Feed list */}
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden" ref={feedRef}>
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Inbox size={16} className="text-slate-400" />
                <span className="font-black text-slate-800 text-sm uppercase tracking-wider">Event feed</span>
              </div>
              <span className="text-[10px] font-black text-slate-400 bg-slate-100 px-3 py-1 rounded-full">
                {filtered.length} {filtered.length === 1 ? 'event' : 'events'}
              </span>
            </div>

            {loading ? (
              <div className="p-20 text-center">
                <Loader2 className="animate-spin text-blue-500 mx-auto" size={28} />
                <p className="text-xs text-slate-400 font-bold uppercase tracking-widest mt-3">
                  Syncing notification ledger...
                </p>
              </div>
            ) : filtered.length === 0 ? (
              <div className="p-20 text-center text-slate-300">
                <BellOff size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">No notifications</p>
                <p className="text-[10px] mt-1 opacity-70">Try clearing filters or refreshing.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-50">
                {filtered.map(n => (
                  <NotificationCard key={n.id} notification={n} isNew={newIds.has(n.id)} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Notifications;
