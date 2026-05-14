import React, { useState } from 'react';
import {
  X, Factory, Settings, Activity, ClipboardCheck,
  CheckCircle2, PauseCircle, XCircle, PlayCircle,
  Users, User, Calendar, History, Package, Trash2,
  PlusCircle, AlertTriangle, ChevronRight, Loader2
} from 'lucide-react';
import productionApi from '../api/productionAxios';
import {
  ProductionOrder, ProductionOrderStatus, ProductionPriority,
  ApiResponse, ProductionMaterial, ProductionStageLog
} from '../types/production';
import toast from 'react-hot-toast';

interface Props {
  order: ProductionOrder | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const statusStyle: Record<ProductionOrderStatus, string> = {
  PLANNED:      'bg-slate-100  text-slate-600  border-slate-200',
  RELEASED:     'bg-blue-50    text-blue-700   border-blue-200',
  IN_PROGRESS:  'bg-amber-50   text-amber-700  border-amber-200',
  QUALITY_CHECK: 'bg-indigo-50  text-indigo-700 border-indigo-200',
  COMPLETED:    'bg-emerald-50 text-emerald-700 border-emerald-200',
  ON_HOLD:      'bg-orange-50  text-orange-700 border-orange-200',
  CANCELLED:    'bg-red-50     text-red-700    border-red-200',
};

export const ProductionOrderDetailModal: React.FC<Props> = ({ order, isOpen, onClose, onSuccess }) => {
  const [activeTab, setActiveTab] = useState<'details' | 'materials' | 'stages'>('details');
  const [loading, setLoading] = useState(false);
  const [actionRemarks, setActionRemarks] = useState('');

  if (!isOpen || !order) return null;

  const handleAction = async (action: string, endpoint: string, params: Record<string, any> = {}) => {
    setLoading(true);
    try {
      await toast.promise(
        productionApi.post<ApiResponse<ProductionOrder>>(`/production/${order.id}/${endpoint}`, null, { params }),
        {
          loading: `${action} in progress...`,
          success: `Order ${order.poNumber} ${action} successfully`,
          error: (err) => err.response?.data?.message || `Failed to ${action}`,
        }
      );
      onSuccess();
    } catch {
    } finally {
      setLoading(false);
    }
  };

  const renderStatusActions = () => {
    switch (order.status) {
      case ProductionOrderStatus.PLANNED:
        return (
          <button
            onClick={() => handleAction('Released', 'release')}
            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-blue-700 transition shadow-lg shadow-blue-200"
          >
            <PlayCircle size={14} /> Release Order
          </button>
        );
      case ProductionOrderStatus.RELEASED:
        return (
          <button
            onClick={() => handleAction('Started', 'start')}
            className="flex items-center gap-2 bg-amber-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-amber-600 transition shadow-lg shadow-amber-200"
          >
            <Activity size={14} /> Start Production
          </button>
        );
      case ProductionOrderStatus.IN_PROGRESS:
        return (
          <div className="flex gap-2">
            <button
              onClick={() => handleAction('Moved to QA', 'quality-check')}
              className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-200"
            >
              <ClipboardCheck size={14} /> Send to QA
            </button>
            <button
              onClick={() => handleAction('Put on Hold', 'hold', { reason: actionRemarks })}
              className="flex items-center gap-2 bg-orange-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-orange-600 transition shadow-lg shadow-orange-200"
            >
              <PauseCircle size={14} /> Put on Hold
            </button>
          </div>
        );
      case ProductionOrderStatus.QUALITY_CHECK:
        return (
          <button
            onClick={() => handleAction('Completed', 'complete', { qualityPassed: true })}
            className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-200"
          >
            <CheckCircle2 size={14} /> Pass QA & Complete
          </button>
        );
      case ProductionOrderStatus.ON_HOLD:
        return (
          <button
            onClick={() => handleAction('Resumed', 'start')}
            className="flex items-center gap-2 bg-amber-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-amber-600 transition shadow-lg shadow-amber-200"
          >
            <PlayCircle size={14} /> Resume Production
          </button>
        );
      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
        
        {/* Header */}
        <div className="px-8 py-6 bg-slate-900 text-white flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="font-mono text-indigo-400 font-black text-lg">{order.poNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[order.status]}`}>
                {order.status.replace('_', ' ')}
              </span>
            </div>
            <h2 className="text-xl font-black">{order.productName}</h2>
            <div className="flex items-center gap-4 mt-2 flex-wrap">
              <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                <Settings size={11} /> {order.productCode || 'N/A'}
              </span>
              <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                <Users size={11} /> {order.assignedTeam || 'No Team Assigned'}
              </span>
            </div>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400">
            <X size={20} />
          </button>
        </div>

        {/* Action Bar */}
        {(order.status !== ProductionOrderStatus.COMPLETED && order.status !== ProductionOrderStatus.CANCELLED) && (
          <div className="px-8 py-4 bg-slate-50 border-b border-slate-100 flex items-center gap-3 flex-wrap">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Available Actions:</span>
            {renderStatusActions()}
            <input
              type="text"
              placeholder="Action notes..."
              className="ml-auto px-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500 flex-1 max-w-[200px]"
              value={actionRemarks}
              onChange={e => setActionRemarks(e.target.value)}
            />
          </div>
        )}

        {/* Tabs */}
        <div className="flex border-b border-slate-100 px-8">
          {(['details', 'materials', 'stages'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-3.5 text-xs font-black uppercase tracking-wider transition border-b-2 -mb-px ${
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              {tab} {tab === 'materials' && `(${order.materials?.length || 0})`}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-8 py-6">
          {activeTab === 'details' && (
            <div className="grid grid-cols-2 gap-4">
              {[
                { label: 'Planned Qty', value: order.plannedQuantity },
                { label: 'Produced Qty', value: order.producedQuantity },
                { label: 'Rejected Qty', value: order.rejectedQuantity },
                { label: 'Priority', value: order.priority },
                { label: 'Supervisor', value: order.supervisorName || '—' },
                { label: 'Production Line', value: order.productionLine || '—' },
                { label: 'Started At', value: order.actualStartDate ? new Date(order.actualStartDate).toLocaleString() : '—' },
                { label: 'Completed At', value: order.actualEndDate ? new Date(order.actualEndDate).toLocaleString() : '—' },
              ].map(item => (
                <div key={item.label} className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{item.label}</p>
                  <p className="text-sm font-bold text-slate-800">{item.value}</p>
                </div>
              ))}
            </div>
          )}

          {activeTab === 'materials' && (
            <div className="space-y-3">
              {(order.materials?.length || 0) === 0 ? (
                <div className="text-center py-10 text-slate-300">
                  <Package size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No materials linked</p>
                </div>
              ) : (
                order.materials.map(m => (
                  <div key={m.id} className="bg-slate-50 p-4 rounded-2xl border border-slate-100 flex items-center justify-between">
                    <div>
                      <p className="font-bold text-slate-800 text-sm">{m.partName}</p>
                      <p className="text-[10px] text-slate-400 font-bold">ID: {m.partId} · Req: {m.requiredQuantity} · Consumed: {m.consumedQuantity}</p>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] font-black px-2 py-1 bg-white border rounded-lg text-slate-600">{m.status}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {activeTab === 'stages' && (
            <div className="space-y-4">
              {(order.stageLogs?.length || 0) === 0 ? (
                <div className="text-center py-10 text-slate-300">
                  <History size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No stage logs recorded</p>
                </div>
              ) : (
                order.stageLogs.map((log, i) => (
                  <div key={log.id} className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-[10px] font-black">
                        {i + 1}
                      </div>
                      {i < order.stageLogs.length - 1 && <div className="w-px flex-1 bg-slate-100 my-1" />}
                    </div>
                    <div className="flex-1 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                      <div className="flex items-center justify-between mb-1">
                        <p className="text-xs font-black text-slate-800">{log.stageName}</p>
                        <span className="text-[10px] font-bold text-slate-400">{new Date(log.startTime).toLocaleString()}</span>
                      </div>
                      <p className="text-[10px] text-slate-500">{log.comments || 'No comments'}</p>
                      <div className="mt-1 flex gap-2">
                         <span className="text-[9px] font-black uppercase text-indigo-500 bg-indigo-50 px-1.5 py-0.5 rounded">Status: {log.status}</span>
                         <span className="text-[9px] font-black uppercase text-slate-400">By: {log.operatorId}</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-8 py-5 border-t border-slate-100 flex justify-end">
           <button
            onClick={onClose}
            className="px-8 py-2.5 bg-slate-900 text-white text-sm font-black rounded-2xl hover:bg-slate-800 transition shadow-lg"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
