import React, { useEffect, useState, useCallback } from 'react';
import {
  Wrench, PlusCircle, Search, RefreshCw, Loader2,
  AlertTriangle, CheckCircle2, Clock, PauseCircle,
  XCircle, PlayCircle, Plane, MapPin, User, Calendar,
  Filter, BarChart3, Package, ChevronRight
} from 'lucide-react';
import workOrderApi from '../api/workOrderAxios';
import {
  WorkOrder, WorkOrderStatus, Priority,
  DashboardStats, ApiResponse
} from '../types/workOrder';
import { CreateWorkOrderModal } from '../components/CreateWorkOrderModal';
import { WorkOrderDetailModal } from '../components/WorkOrderDetailModal';
import toast from 'react-hot-toast';

// ── Style maps ────────────────────────────────────────────────────────────────

const statusStyle: Record<WorkOrderStatus, string> = {
  DRAFT:       'bg-slate-100  text-slate-600  border-slate-200',
  OPEN:        'bg-blue-50    text-blue-700   border-blue-200',
  IN_PROGRESS: 'bg-amber-50   text-amber-700  border-amber-200',
  ON_HOLD:     'bg-orange-50  text-orange-700 border-orange-200',
  COMPLETED:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED:   'bg-red-50     text-red-700    border-red-200',
};

const statusIcon: Record<WorkOrderStatus, React.ReactNode> = {
  DRAFT:       <Clock size={12} />,
  OPEN:        <PlayCircle size={12} />,
  IN_PROGRESS: <PlayCircle size={12} />,
  ON_HOLD:     <PauseCircle size={12} />,
  COMPLETED:   <CheckCircle2 size={12} />,
  CANCELLED:   <XCircle size={12} />,
};

const priorityStyle: Record<Priority, string> = {
  LOW:      'text-emerald-600 bg-emerald-50',
  MEDIUM:   'text-amber-600   bg-amber-50',
  HIGH:     'text-orange-600  bg-orange-50',
  CRITICAL: 'text-red-600     bg-red-50 font-black',
};

const STATUS_FILTERS: { key: WorkOrderStatus | 'ALL'; label: string }[] = [
  { key: 'ALL',         label: 'All'         },
  { key: 'DRAFT',       label: 'Draft'       },
  { key: 'OPEN',        label: 'Open'        },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'ON_HOLD',     label: 'On Hold'     },
  { key: 'COMPLETED',   label: 'Completed'   },
  { key: 'CANCELLED',   label: 'Cancelled'   },
];

// ── WorkOrderCard ─────────────────────────────────────────────────────────────

interface CardProps {
  wo: WorkOrder;
  onClick: () => void;
}

const WorkOrderCard: React.FC<CardProps> = ({ wo, onClick }) => {
  const isOverdue = wo.dueDate && new Date(wo.dueDate) < new Date()
    && wo.status !== 'COMPLETED' && wo.status !== 'CANCELLED';

  const progress = wo.totalPartsRequired > 0
    ? Math.round((wo.totalPartsIssued / wo.totalPartsRequired) * 100)
    : 0;

  return (
    <div
      onClick={onClick}
      className={`group bg-white rounded-3xl p-6 shadow-sm border hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer
        ${isOverdue ? 'border-red-200 bg-red-50/20' : 'border-slate-100'}`}
    >
      {/* Top row */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-mono text-xs font-black text-blue-600 bg-blue-50 px-2.5 py-1 rounded-lg border border-blue-100">
            {wo.woNumber}
          </span>
          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-black border ${statusStyle[wo.status]}`}>
            {statusIcon[wo.status]}
            {wo.status.replace('_', ' ')}
          </span>
          {isOverdue && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-black bg-red-500 text-white">
              <AlertTriangle size={9} /> OVERDUE
            </span>
          )}
        </div>
        <span className={`text-[10px] font-black px-2 py-1 rounded-lg ${priorityStyle[wo.priority]}`}>
          {wo.priority === 'CRITICAL' ? '⚡ AOG' : wo.priority}
        </span>
      </div>

      {/* Task */}
      <h3 className="font-black text-slate-900 text-sm mb-1 line-clamp-2 group-hover:text-blue-600 transition-colors">
        {wo.taskDescription}
      </h3>

      {/* Meta */}
      <div className="flex items-center gap-3 mt-2 flex-wrap">
        <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
          <Plane size={10} /> {wo.aircraftId}
          {wo.aircraftType ? ` · ${wo.aircraftType}` : ''}
        </span>
        {wo.location && (
          <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
            <MapPin size={10} /> {wo.location}
          </span>
        )}
        {wo.assignedTo && (
          <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
            <User size={10} /> {wo.assignedTo}
          </span>
        )}
        {wo.dueDate && (
          <span className={`flex items-center gap-1 text-[10px] font-bold ${isOverdue ? 'text-red-500' : 'text-slate-400'}`}>
            <Calendar size={10} /> {new Date(wo.dueDate).toLocaleDateString()}
          </span>
        )}
      </div>

      {/* Material progress bar */}
      {wo.totalPartsRequired > 0 && (
        <div className="mt-4">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[10px] text-slate-400 font-bold">
              Material: {wo.totalPartsIssued}/{wo.totalPartsRequired} parts issued
            </span>
            <span className="text-[10px] font-black text-slate-600">{progress}%</span>
          </div>
          <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                progress === 100 ? 'bg-emerald-500' : 'bg-blue-500'
              }`}
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      <div className="flex items-center justify-between mt-3">
        <span className="text-[10px] text-slate-400 font-bold">
          {new Date(wo.createdAt).toLocaleDateString()}
        </span>
        <ChevronRight size={14} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
      </div>
    </div>
  );
};

// ── Main Page ─────────────────────────────────────────────────────────────────

const WorkOrders: React.FC = () => {
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<WorkOrderStatus | 'ALL'>('ALL');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedWO, setSelectedWO] = useState<WorkOrder | null>(null);
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [editingWO, setEditingWO] = useState<WorkOrder | null>(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    setOverdueOnly(false);
    try {
      const [woRes, statsRes] = await Promise.all([
        workOrderApi.get<ApiResponse<WorkOrder[]>>('/work-orders'),
        workOrderApi.get<ApiResponse<DashboardStats>>('/work-orders/dashboard/stats'),
      ]);
      setWorkOrders(woRes.data.data);
      setStats(statsRes.data.data);
    } catch {
      // interceptor handles toast
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchByStatus = useCallback(async (status: WorkOrderStatus | 'ALL') => {
    setLoading(true);
    try {
      const url = status === 'ALL' ? '/work-orders' : `/work-orders/status/${status}`;
      const res = await workOrderApi.get<ApiResponse<WorkOrder[]>>(url);
      setWorkOrders(res.data.data);
    } catch {
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchBySearch = useCallback(async (q: string) => {
    if (!q.trim()) { fetchByStatus(statusFilter); return; }
    setOverdueOnly(false);
    setLoading(true);
    try {
      const res = await workOrderApi.get<ApiResponse<WorkOrder[]>>(
        `/work-orders/search?q=${encodeURIComponent(q)}`
      );
      setWorkOrders(res.data.data);
    } catch {
    } finally {
      setLoading(false);
    }
  }, [statusFilter, fetchByStatus]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  useEffect(() => {
    const t = setTimeout(() => fetchBySearch(searchTerm), 300);
    return () => clearTimeout(t);
  }, [searchTerm, fetchBySearch]);

  const handleStatusFilter = (s: WorkOrderStatus | 'ALL') => {
    setOverdueOnly(false);
    setStatusFilter(s);
    setSearchTerm('');
    fetchByStatus(s);
  };

  // The banner's overdueCount comes from /dashboard/stats, which correctly excludes
  // COMPLETED/CANCELLED — but "View Active" used to just filter to IN_PROGRESS, missing
  // OPEN/ON_HOLD/DRAFT work orders that are also overdue. Hit the dedicated endpoint instead
  // so this always shows exactly the work orders the banner is warning about.
  const handleViewOverdue = async () => {
    setLoading(true);
    setSearchTerm('');
    setStatusFilter('ALL');
    try {
      const res = await workOrderApi.get<ApiResponse<WorkOrder[]>>('/work-orders/overdue');
      setWorkOrders(res.data.data);
      setOverdueOnly(true);
    } catch {
    } finally {
      setLoading(false);
    }
  };

  const handleSuccess = () => {
    fetchAll();
    // Refresh selected WO if detail modal is open
    if (selectedWO) {
      workOrderApi.get<ApiResponse<WorkOrder>>(`/work-orders/${selectedWO.id}`)
        .then(r => setSelectedWO(r.data.data))
        .catch(() => {});
    }
  };

  const handleOpenEditWO = (wo: WorkOrder) => {
    setSelectedWO(null);
    setEditingWO(wo);
    setIsCreateOpen(true);
  };

  // Only a DRAFT work order can be deleted (enforced server-side too) — anything past
  // DRAFT may already have material issued against it, which must stay in the ledger.
  const handleDeleteWO = async (wo: WorkOrder) => {
    if (!window.confirm(`Delete draft work order ${wo.woNumber}? This cannot be undone.`)) return;
    try {
      await workOrderApi.delete(`/work-orders/${wo.id}`);
      toast.success(`${wo.woNumber} deleted.`);
      setSelectedWO(null);
      fetchAll();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Delete failed.');
    }
  };

  const statCards = stats ? [
    { title: 'Total WOs',    value: stats.totalWorkOrders, icon: <Wrench size={20} />,       color: 'from-blue-600 to-blue-700',     note: 'All work orders'       },
    { title: 'Open',         value: stats.open,            icon: <PlayCircle size={20} />,    color: 'from-slate-700 to-slate-800',   note: 'Awaiting start'        },
    { title: 'In Progress',  value: stats.inProgress,      icon: <BarChart3 size={20} />,     color: 'from-amber-500 to-amber-600',   note: 'Currently active'      },
    { title: 'On Hold',      value: stats.onHold,          icon: <PauseCircle size={20} />,   color: 'from-orange-500 to-orange-600', note: 'Blocked / waiting'     },
    { title: 'Completed',    value: stats.completed,       icon: <CheckCircle2 size={20} />,  color: 'from-emerald-600 to-emerald-700', note: 'Successfully closed' },
    { title: 'AOG / CRIT',  value: stats.criticalPriority, icon: <AlertTriangle size={20} />, color: 'from-red-600 to-red-700',      note: 'Aircraft on ground'    },
  ] : [];

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">

      <CreateWorkOrderModal
        isOpen={isCreateOpen}
        onClose={() => { setIsCreateOpen(false); setEditingWO(null); }}
        onSuccess={handleSuccess}
        editingWorkOrder={editingWO}
      />
      <WorkOrderDetailModal
        workOrder={selectedWO}
        isOpen={!!selectedWO}
        onClose={() => setSelectedWO(null)}
        onSuccess={handleSuccess}
        onEdit={handleOpenEditWO}
        onDelete={handleDeleteWO}
      />

      {/* ── Header ── */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-gradient-to-br from-blue-600 to-blue-700 rounded-2xl text-white shadow-lg shadow-blue-200">
              <Wrench size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Work Orders</h1>
              <p className="text-slate-500 text-sm font-medium">
                Aircraft maintenance tasks — material tracking and status lifecycle.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={fetchAll}
              className="p-2 hover:bg-slate-100 rounded-xl transition text-slate-400 hover:text-slate-700"
              title="Refresh"
            >
              <RefreshCw size={16} />
            </button>
            <button
              onClick={() => setIsCreateOpen(true)}
              className="flex items-center gap-2 bg-blue-600 text-white px-6 py-2.5 rounded-2xl font-bold text-sm hover:bg-blue-700 transition shadow-lg shadow-blue-200"
            >
              <PlusCircle size={18} /> New Work Order
            </button>
          </div>
        </div>
      </div>

      {/* ── KPI Cards ── */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {statCards.map((c, i) => (
            <div key={i} className="group bg-white rounded-3xl p-6 shadow-sm border border-slate-100 hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
              <div className={`p-4 rounded-2xl bg-gradient-to-br ${c.color} text-white shadow-lg w-fit`}>
                {c.icon}
              </div>
              <div className="mt-5">
                <p className="text-[11px] font-black text-slate-400 uppercase tracking-[0.15em] mb-1">{c.title}</p>
                <p className="text-3xl font-black text-slate-900 tracking-tight">{c.value}</p>
                <p className="text-[10px] text-slate-400 mt-1 font-medium">{c.note}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Overdue Banner ── */}
      {stats && stats.overdueCount > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-3xl p-4 flex items-center gap-3">
          <AlertTriangle size={18} className="text-red-600 flex-shrink-0" />
          <p className="text-sm font-bold text-red-700">
            {stats.overdueCount} work order{stats.overdueCount > 1 ? 's are' : ' is'} past due date and still active.
          </p>
          <button
            onClick={handleViewOverdue}
            className="ml-auto text-xs font-black text-red-600 hover:text-red-800 transition underline"
          >
            View Overdue
          </button>
        </div>
      )}

      {overdueOnly && (
        <div className="bg-slate-100 border border-slate-200 rounded-2xl px-4 py-2.5 flex items-center gap-3">
          <span className="text-xs font-bold text-slate-600">
            Showing only overdue, active work orders ({workOrders.length}).
          </span>
          <button
            onClick={() => handleStatusFilter('ALL')}
            className="ml-auto text-xs font-black text-slate-500 hover:text-slate-800 transition underline"
          >
            Clear
          </button>
        </div>
      )}

      {/* ── Toolbar ── */}
      <div className="bg-white rounded-3xl p-5 shadow-sm border border-slate-100 flex flex-col md:flex-row items-center gap-4">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <Search className="absolute left-4 top-3 text-slate-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search WO number, aircraft, task..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-blue-500 font-medium outline-none text-sm"
          />
        </div>

        {/* Status filter tabs */}
        <div className="flex items-center gap-1.5 flex-wrap flex-1">
          <Filter size={13} className="text-slate-400 flex-shrink-0" />
          {STATUS_FILTERS.map(f => (
            <button
              key={f.key}
              onClick={() => handleStatusFilter(f.key as WorkOrderStatus | 'ALL')}
              className={`px-3 py-1.5 rounded-xl text-[11px] font-black transition-all border ${
                statusFilter === f.key
                  ? 'bg-slate-900 text-white border-slate-900'
                  : 'bg-white text-slate-500 border-slate-200 hover:border-slate-300'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* ── Work Order Cards Grid ── */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="animate-spin text-blue-500" size={32} />
        </div>
      ) : workOrders.length === 0 ? (
        <div className="bg-white rounded-3xl p-20 text-center border border-slate-100 shadow-sm">
          <Wrench size={48} className="mx-auto mb-3 text-slate-200" />
          <p className="font-bold text-slate-400 uppercase tracking-widest text-xs">No work orders found</p>
          <p className="text-slate-400 text-xs mt-1">Create your first work order or clear the filters.</p>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="mt-4 px-6 py-2.5 bg-blue-600 text-white text-sm font-black rounded-2xl hover:bg-blue-700 transition shadow-lg shadow-blue-200"
          >
            Create Work Order
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {workOrders.map(wo => (
            <WorkOrderCard
              key={wo.id}
              wo={wo}
              onClick={() => setSelectedWO(wo)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default WorkOrders;
