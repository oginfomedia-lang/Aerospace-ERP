import React, { useEffect, useState, useCallback } from 'react';
import {
  Factory, PlusCircle, Search, RefreshCw, Loader2,
  AlertTriangle, CheckCircle2, Clock, PauseCircle,
  XCircle, PlayCircle, ClipboardCheck, Settings, Users, Calendar,
  Filter, BarChart3, Package, ChevronRight, Activity
} from 'lucide-react';
import productionApi from '../api/productionAxios';
import {
  ProductionOrder, ProductionOrderStatus, ProductionPriority,
  DashboardStats, ApiResponse
} from '../types/production';
import { CreateProductionOrderModal } from '../components/CreateProductionOrderModal';
import { ProductionOrderDetailModal } from '../components/ProductionOrderDetailModal';
import toast from 'react-hot-toast';

// ── Style maps ────────────────────────────────────────────────────────────────

const statusStyle: Record<ProductionOrderStatus, string> = {
  PLANNED:      'bg-slate-100  text-slate-600  border-slate-200',
  RELEASED:     'bg-blue-50    text-blue-700   border-blue-200',
  IN_PRODUCTION:  'bg-amber-50   text-amber-700  border-amber-200',
  QUALITY_CHECK: 'bg-indigo-50  text-indigo-700 border-indigo-200',
  COMPLETED:    'bg-emerald-50 text-emerald-700 border-emerald-200',
  ON_HOLD:      'bg-orange-50  text-orange-700 border-orange-200',
  CANCELLED:    'bg-red-50     text-red-700    border-red-200',
};

const statusIcon: Record<ProductionOrderStatus, React.ReactNode> = {
  PLANNED:      <Clock size={12} />,
  RELEASED:     <PlayCircle size={12} />,
  IN_PRODUCTION:  <Activity size={12} />,
  QUALITY_CHECK: <ClipboardCheck size={12} />,
  COMPLETED:    <CheckCircle2 size={12} />,
  ON_HOLD:      <PauseCircle size={12} />,
  CANCELLED:    <XCircle size={12} />,
};

const priorityStyle: Record<ProductionPriority, string> = {
  LOW:      'text-emerald-600 bg-emerald-50',
  MEDIUM:   'text-amber-600   bg-amber-50',
  HIGH:     'text-orange-600  bg-orange-50',
  URGENT:   'text-red-600     bg-red-50 font-black',
};

const STATUS_FILTERS: { key: ProductionOrderStatus | 'ALL'; label: string }[] = [
  { key: 'ALL',          label: 'All'           },
  { key: ProductionOrderStatus.PLANNED,      label: 'Planned'       },
  { key: ProductionOrderStatus.RELEASED,     label: 'Released'      },
  { key: ProductionOrderStatus.IN_PRODUCTION,  label: 'In Progress'   },
  { key: ProductionOrderStatus.QUALITY_CHECK, label: 'Quality Check' },
  { key: ProductionOrderStatus.COMPLETED,    label: 'Completed'     },
  { key: ProductionOrderStatus.ON_HOLD,      label: 'On Hold'       },
];

// ── ProductionOrderCard ─────────────────────────────────────────────────────────────

interface CardProps {
  order: ProductionOrder;
  onClick: () => void;
}

const ProductionOrderCard: React.FC<CardProps> = ({ order, onClick }) => {
  const isOverdue = order.plannedEndDate && new Date(order.plannedEndDate) < new Date()
    && order.status !== ProductionOrderStatus.COMPLETED && order.status !== ProductionOrderStatus.CANCELLED;

  const progress = order.plannedQuantity > 0
    ? Math.round((order.producedQuantity / order.plannedQuantity) * 100)
    : 0;

  return (
    <div
      onClick={onClick}
      className={`group bg-white rounded-3xl p-6 shadow-sm border hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer
        ${isOverdue ? 'border-red-200 bg-red-50/20' : 'border-slate-100'}`}
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-mono text-xs font-black text-indigo-600 bg-indigo-50 px-2.5 py-1 rounded-lg border border-indigo-100">
            {order.poNumber}
          </span>
          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-black border ${statusStyle[order.status]}`}>
            {statusIcon[order.status]}
            {order.status.replace('_', ' ')}
          </span>
          {isOverdue && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-black bg-red-500 text-white">
              <AlertTriangle size={9} /> OVERDUE
            </span>
          )}
        </div>
        <span className={`text-[10px] font-black px-2 py-1 rounded-lg ${priorityStyle[order.priority]}`}>
          {order.priority}
        </span>
      </div>

      <h3 className="font-black text-slate-900 text-sm mb-1 line-clamp-2 group-hover:text-indigo-600 transition-colors">
        {order.productName}
      </h3>

      <div className="flex items-center gap-3 mt-2 flex-wrap">
        <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
          <Settings size={10} /> {order.productCode || 'N/A'}
        </span>
        {order.aircraftType && (
          <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
            <BarChart3 size={10} /> {order.aircraftType}
          </span>
        )}
        {order.assignedTeam && (
          <span className="flex items-center gap-1 text-[10px] text-slate-400 font-bold">
            <Users size={10} /> {order.assignedTeam}
          </span>
        )}
        {order.plannedEndDate && (
          <span className={`flex items-center gap-1 text-[10px] font-bold ${isOverdue ? 'text-red-500' : 'text-slate-400'}`}>
            <Calendar size={10} /> {new Date(order.plannedEndDate).toLocaleDateString()}
          </span>
        )}
      </div>

      <div className="mt-4">
        <div className="flex items-center justify-between mb-1.5">
          <span className="text-[10px] text-slate-400 font-bold">
            Production: {order.producedQuantity}/{order.plannedQuantity} units
          </span>
          <span className="text-[10px] font-black text-slate-600">{progress}%</span>
        </div>
        <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all duration-500 ${
              progress === 100 ? 'bg-emerald-500' : 'bg-indigo-500'
            }`}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="flex items-center justify-between mt-3">
        <span className="text-[10px] text-slate-400 font-bold">
          {new Date(order.createdAt).toLocaleDateString()}
        </span>
        <ChevronRight size={14} className="text-slate-300 group-hover:text-indigo-500 transition-colors" />
      </div>
    </div>
  );
};

// ── Main Page ─────────────────────────────────────────────────────────────────

const ProductionOrders: React.FC = () => {
  const [orders, setOrders] = useState<ProductionOrder[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProductionOrderStatus | 'ALL'>('ALL');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<ProductionOrder | null>(null);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [orderRes, statsRes] = await Promise.all([
        productionApi.get<ApiResponse<ProductionOrder[]>>('/production'),
        productionApi.get<ApiResponse<DashboardStats>>('/production/dashboard/stats'),
      ]);
      setOrders(orderRes.data.data);
      setStats(statsRes.data.data);
    } catch {
      // toast handled by axios
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchByStatus = useCallback(async (status: ProductionOrderStatus | 'ALL') => {
    setLoading(true);
    try {
      const url = status === 'ALL' ? '/production' : `/production/by-status?status=${status}`;
      const res = await productionApi.get<ApiResponse<ProductionOrder[]>>(url);
      setOrders(res.data.data);
    } catch {
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchBySearch = useCallback(async (q: string) => {
    if (!q.trim()) { fetchByStatus(statusFilter); return; }
    setLoading(true);
    try {
      const res = await productionApi.get<ApiResponse<ProductionOrder[]>>(
        `/production/search?productName=${encodeURIComponent(q)}`
      );
      setOrders(res.data.data);
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

  const handleStatusFilter = (s: ProductionOrderStatus | 'ALL') => {
    setStatusFilter(s);
    setSearchTerm('');
    fetchByStatus(s);
  };

  const statCards = stats ? [
    { title: 'Total Orders',  value: stats.totalOrders,        icon: <Factory size={20} />,      color: 'from-indigo-600 to-indigo-700' },
    { title: 'In Production', value: stats.inProductionOrders, icon: <Activity size={20} />,     color: 'from-amber-500 to-amber-600' },
    { title: 'Completed',     value: stats.completedOrders,    icon: <CheckCircle2 size={20} />,  color: 'from-emerald-600 to-emerald-700' },
    { title: 'On Hold',       value: stats.onHoldOrders,       icon: <PauseCircle size={20} />,   color: 'from-orange-500 to-orange-600' },
    { title: 'QA Check',      value: stats.qualityCheckOrders, icon: <ClipboardCheck size={20} />, color: 'from-blue-500 to-blue-600' },
    { title: 'Success Rate',  value: `${Math.round(stats.overallCompletionRate)}%`, icon: <BarChart3 size={20} />, color: 'from-blue-600 to-blue-700' },
  ] : [];

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
      
      <CreateProductionOrderModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={fetchAll}
      />
      <ProductionOrderDetailModal
        order={selectedOrder}
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        onSuccess={() => {
          fetchAll();
          if (selectedOrder) {
            productionApi.get<ApiResponse<ProductionOrder>>(`/production/${selectedOrder.id}`)
              .then(r => setSelectedOrder(r.data.data))
              .catch(() => {});
          }
        }}
      />

      {/* Header */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-gradient-to-br from-indigo-600 to-indigo-700 rounded-2xl text-white shadow-lg shadow-indigo-200">
              <Factory size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Production Management</h1>
              <p className="text-slate-500 text-sm font-medium">
                Track manufacturing orders, assembly lines, and quality control.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={fetchAll}
              className="p-2 hover:bg-slate-100 rounded-xl transition text-slate-400 hover:text-slate-700"
            >
              <RefreshCw size={16} />
            </button>
            <button
              onClick={() => setIsCreateOpen(true)}
              className="flex items-center gap-2 bg-indigo-600 text-white px-6 py-2.5 rounded-2xl font-bold text-sm hover:bg-indigo-700 transition shadow-lg shadow-indigo-200"
            >
              <PlusCircle size={18} /> Create Order
            </button>
          </div>
        </div>
      </div>

      {/* Stats */}
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
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Toolbar */}
      <div className="bg-white rounded-3xl p-5 shadow-sm border border-slate-100 flex flex-col md:flex-row items-center gap-4">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-4 top-3 text-slate-400 w-4 h-4" />
          <input
            type="text"
            placeholder="Search by product name..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="w-full pl-11 pr-4 py-2.5 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-indigo-500 font-medium outline-none text-sm"
          />
        </div>

        <div className="flex items-center gap-1.5 flex-wrap flex-1">
          <Filter size={13} className="text-slate-400 flex-shrink-0" />
          {STATUS_FILTERS.map(f => (
            <button
              key={f.key}
              onClick={() => handleStatusFilter(f.key as ProductionOrderStatus | 'ALL')}
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

      {/* Grid */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="animate-spin text-indigo-500" size={32} />
        </div>
      ) : orders.length === 0 ? (
        <div className="bg-white rounded-3xl p-20 text-center border border-slate-100 shadow-sm">
          <Factory size={48} className="mx-auto mb-3 text-slate-200" />
          <p className="font-bold text-slate-400 uppercase tracking-widest text-xs">No production orders found</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {orders.map(order => (
            <ProductionOrderCard
              key={order.id}
              order={order}
              onClick={() => setSelectedOrder(order)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default ProductionOrders;
