import React, { useEffect, useState } from 'react';
import {
  X, Send, Check, XCircle, PackageCheck, Truck, RotateCcw, Gauge,
  Loader2, PlusCircle, History,
} from 'lucide-react';
import salesApi from '../api/salesAxios';
import toast from 'react-hot-toast';
import { ApiResponse } from '../types/inventory';
import {
  SalesOrder, AtpResult, Dispatch, ReturnAuthorization, OrderTracking,
  ReturnAuthorizationRequest,
} from '../types/sales';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_SALES, CAN_FULFILL_ORDERS, CAN_MANAGE_SALES } from '../utils/permissions';

interface Props {
  order: SalesOrder | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void; // refetch the list after any state-changing action
}

const statusStyle: Record<string, string> = {
  DRAFT:            'bg-slate-100  text-slate-600  border-slate-200',
  PENDING_APPROVAL: 'bg-amber-50   text-amber-700  border-amber-200',
  APPROVED:         'bg-sky-50     text-sky-700    border-sky-200',
  REJECTED:         'bg-red-50     text-red-700    border-red-200',
  CONFIRMED:        'bg-blue-50    text-blue-700   border-blue-200',
  ALLOCATED:        'bg-violet-50  text-violet-700 border-violet-200',
  IN_FULFILLMENT:   'bg-orange-50  text-orange-700 border-orange-200',
  DISPATCHED:       'bg-indigo-50  text-indigo-700 border-indigo-200',
  DELIVERED:        'bg-emerald-50 text-emerald-700 border-emerald-200',
  CLOSED:           'bg-slate-100  text-slate-500  border-slate-200',
  CANCELLED:        'bg-red-50     text-red-700    border-red-200',
};

const emptyReturnForm = (salesOrderId: number): ReturnAuthorizationRequest => ({
  salesOrderId, partId: 0, partNumber: '', quantity: 1, reasonCode: '', reasonDescription: '',
});

export const SalesOrderDetailModal: React.FC<Props> = ({ order, isOpen, onClose, onSuccess }) => {
  const { user } = useAuth();
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_SALES);
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_SALES);
  const canFulfill = hasAnyRole(user?.roles, CAN_FULFILL_ORDERS);

  const [activeTab, setActiveTab] = useState<'details' | 'lines' | 'atp' | 'fulfillment' | 'returns'>('details');
  const [acting, setActing] = useState(false);
  const [history, setHistory] = useState<SalesOrder[]>([]);

  const [atpResults, setAtpResults] = useState<AtpResult[] | null>(null);
  const [runningAtp, setRunningAtp] = useState(false);

  const [dispatches, setDispatches] = useState<Dispatch[]>([]);
  const [tracking, setTracking] = useState<OrderTracking | null>(null);
  const [carrierName, setCarrierName] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [confirmingDispatchId, setConfirmingDispatchId] = useState<number | null>(null);

  const [returns, setReturns] = useState<ReturnAuthorization[]>([]);
  const [returnForm, setReturnForm] = useState<ReturnAuthorizationRequest>(emptyReturnForm(order?.id || 0));
  const [showReturnForm, setShowReturnForm] = useState(false);

  useEffect(() => {
    if (!order || !isOpen) return;
    setActiveTab('details');
    setAtpResults(null);
    setReturnForm(emptyReturnForm(order.id));
    setShowReturnForm(false);
    salesApi.get<ApiResponse<SalesOrder[]>>(`/sales/orders/${order.id}/history`)
      .then(res => setHistory(res.data.data)).catch(() => setHistory([]));
    salesApi.get<ApiResponse<Dispatch[]>>(`/sales/dispatch/by-order/${order.id}`)
      .then(res => setDispatches(res.data.data)).catch(() => setDispatches([]));
    salesApi.get<ApiResponse<OrderTracking>>(`/sales/orders/${order.id}/tracking`)
      .then(res => setTracking(res.data.data)).catch(() => setTracking(null));
    salesApi.get<ApiResponse<ReturnAuthorization[]>>('/sales/rma')
      .then(res => setReturns(res.data.data.filter(r => r.salesOrderId === order.id))).catch(() => setReturns([]));
  }, [order, isOpen]);

  if (!isOpen || !order) return null;

  const runOrderAction = async (action: 'submit' | 'approve' | 'reject' | 'allocate') => {
    setActing(true);
    try {
      await toast.promise(
        salesApi.post(`/sales/orders/${order.id}/${action}`, {}),
        {
          loading: 'Working...',
          success: `Order ${action}${action.endsWith('e') ? 'd' : 'ed'}.`,
          error: (err) => err.response?.data?.message || `Failed to ${action} order.`,
        }
      );
      onSuccess();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const runAtp = async () => {
    setRunningAtp(true);
    try {
      const res = await salesApi.get<ApiResponse<AtpResult[]>>(`/sales/orders/${order.id}/atp`);
      setAtpResults(res.data.data);
      toast.success('ATP evaluated — promised delivery date updated.');
      onSuccess();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to evaluate ATP.');
    } finally {
      setRunningAtp(false);
    }
  };

  const refreshFulfillment = async () => {
    const [dRes, tRes] = await Promise.all([
      salesApi.get<ApiResponse<Dispatch[]>>(`/sales/dispatch/by-order/${order.id}`),
      salesApi.get<ApiResponse<OrderTracking>>(`/sales/orders/${order.id}/tracking`),
    ]);
    setDispatches(dRes.data.data);
    setTracking(tRes.data.data);
  };

  const createPickList = async () => {
    setActing(true);
    try {
      await toast.promise(
        salesApi.post(`/sales/dispatch?orderId=${order.id}`),
        { loading: 'Creating pick list...', success: 'Pick list created.', error: (err) => err.response?.data?.message || 'Failed to create pick list.' }
      );
      await refreshFulfillment();
      onSuccess();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const markPacked = async (dispatchId: number) => {
    setActing(true);
    try {
      await toast.promise(
        salesApi.post(`/sales/dispatch/${dispatchId}/pack`),
        { loading: 'Marking packed...', success: 'Marked packed.', error: (err) => err.response?.data?.message || 'Failed to mark packed.' }
      );
      await refreshFulfillment();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const confirmDispatch = async (dispatchId: number) => {
    if (!carrierName.trim() || !trackingNumber.trim()) { toast.error('Carrier name and tracking number are required.'); return; }
    setActing(true);
    try {
      await toast.promise(
        salesApi.post(`/sales/dispatch/${dispatchId}/confirm`, { carrierName, trackingNumber }),
        { loading: 'Confirming dispatch...', success: 'Dispatch confirmed — stock shipped.', error: (err) => err.response?.data?.message || 'Failed to confirm dispatch.' }
      );
      setConfirmingDispatchId(null);
      setCarrierName(''); setTrackingNumber('');
      await refreshFulfillment();
      onSuccess();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const authorizeReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!returnForm.partId || !returnForm.quantity) { toast.error('Part and quantity are required.'); return; }
    setActing(true);
    try {
      await toast.promise(
        salesApi.post('/sales/rma', returnForm),
        { loading: 'Authorizing return...', success: 'Return authorized.', error: (err) => err.response?.data?.message || 'Failed to authorize return.' }
      );
      setShowReturnForm(false);
      setReturnForm(emptyReturnForm(order.id));
      const res = await salesApi.get<ApiResponse<ReturnAuthorization[]>>('/sales/rma');
      setReturns(res.data.data.filter(r => r.salesOrderId === order.id));
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="px-8 py-6 bg-slate-900 text-white flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="font-mono text-blue-400 font-black text-lg">{order.orderNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[order.status] || ''}`}>
                {order.status.replace(/_/g, ' ')}
              </span>
              {order.revisionNumber > 1 && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black bg-white/10 text-slate-300 border border-white/20">
                  Rev {order.revisionNumber}
                </span>
              )}
            </div>
            <p className="text-slate-300 text-sm font-medium">{order.customerName}</p>
            <div className="flex items-center gap-4 mt-2 flex-wrap text-[11px] text-slate-400 font-bold">
              <span>{order.orderType || 'STANDARD'}</span>
              {order.priority && <span>Priority: {order.priority}</span>}
              {order.promisedDeliveryDate && <span>Promised: {order.promisedDeliveryDate}</span>}
              <span>{order.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400">
            <X size={20} />
          </button>
        </div>

        {/* Status action bar */}
        <div className="px-8 py-4 bg-slate-50 border-b border-slate-100 flex items-center gap-3 flex-wrap">
          <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Actions:</span>
          {order.status === 'DRAFT' && canManage && (
            <button disabled={acting} onClick={() => runOrderAction('submit')}
              className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-blue-600 hover:bg-blue-700 text-white shadow-blue-200">
              <Send size={13} /> Submit for Approval
            </button>
          )}
          {order.status === 'PENDING_APPROVAL' && canApprove && (
            <>
              <button disabled={acting} onClick={() => runOrderAction('approve')}
                className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200">
                <Check size={13} /> Approve
              </button>
              <button disabled={acting} onClick={() => runOrderAction('reject')}
                className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-red-500 hover:bg-red-600 text-white shadow-red-200">
                <XCircle size={13} /> Reject
              </button>
            </>
          )}
          {order.status === 'CONFIRMED' && canFulfill && (
            <button disabled={acting} onClick={() => runOrderAction('allocate')}
              className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-violet-600 hover:bg-violet-700 text-white shadow-violet-200">
              <PackageCheck size={13} /> Allocate Inventory
            </button>
          )}
          {!['DRAFT', 'PENDING_APPROVAL', 'CONFIRMED'].includes(order.status) && (
            <span className="text-[11px] text-slate-400 font-bold">No status actions available right now.</span>
          )}
        </div>

        {/* Tabs */}
        <div className="flex border-b border-slate-100 px-8 overflow-x-auto">
          {(['details', 'lines', 'atp', 'fulfillment', 'returns'] as const).map(tab => (
            <button key={tab} onClick={() => setActiveTab(tab)}
              className={`px-4 py-3.5 text-xs font-black uppercase tracking-wider transition border-b-2 -mb-px whitespace-nowrap ${
                activeTab === tab ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}>
              {tab === 'lines' ? `Lines (${order.lines.length})` :
               tab === 'atp' ? 'ATP' :
               tab === 'fulfillment' ? `Fulfillment (${dispatches.length})` :
               tab === 'returns' ? `Returns (${returns.length})` : tab}
            </button>
          ))}
        </div>

        {/* Tab content */}
        <div className="flex-1 overflow-y-auto px-8 py-6">

          {/* Details */}
          {activeTab === 'details' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                {[
                  { label: 'Order Type', value: order.orderType || '—' },
                  { label: 'Priority', value: order.priority || '—' },
                  { label: 'Requested Delivery', value: order.requestedDeliveryDate || '—' },
                  { label: 'Promised Delivery', value: order.promisedDeliveryDate || 'Not yet evaluated (run ATP)' },
                  { label: 'Shipping Address', value: order.shippingAddress || '—' },
                  { label: 'Source Quotation', value: order.sourceQuotationId ? `#${order.sourceQuotationId}` : '—' },
                  { label: 'Created By', value: order.createdBy || '—' },
                  { label: 'Created At', value: new Date(order.createdAt).toLocaleString() },
                ].map(row => (
                  <div key={row.label} className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{row.label}</p>
                    <p className="text-sm font-bold text-slate-800">{row.value}</p>
                  </div>
                ))}
              </div>

              {history.length > 1 && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <History size={14} className="text-slate-400" />
                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Revision History</span>
                  </div>
                  <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
                    {history.map(h => (
                      <div key={h.id} className="p-3 flex items-center justify-between text-xs">
                        <span className="font-bold text-slate-700">Rev {h.revisionNumber}{h.isCurrentRevision ? ' (current)' : ''}</span>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[h.status] || ''}`}>{h.status}</span>
                        <span className="text-slate-400">{h.totalAmount.toFixed(2)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Lines */}
          {activeTab === 'lines' && (
            <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
              {order.lines.map(l => (
                <div key={l.id} className="p-4 grid grid-cols-5 gap-2 text-xs items-center">
                  <div className="col-span-2">
                    <div className="font-bold text-slate-800">{l.partName || l.partNumber || `Part #${l.partId}`}</div>
                    <div className="text-slate-400">{l.partNumber}</div>
                  </div>
                  <div className="text-slate-600">{l.quantity} × {l.unitPrice.toFixed(2)}</div>
                  <div className="text-slate-600">Allocated: {l.quantityAllocated}/{l.quantity}</div>
                  <div className="text-right font-black text-slate-700">Shipped: {l.quantityShipped}/{l.quantity}</div>
                </div>
              ))}
            </div>
          )}

          {/* ATP */}
          {activeTab === 'atp' && (
            <div className="space-y-4">
              <button onClick={runAtp} disabled={runningAtp}
                className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-xs font-black rounded-xl hover:bg-blue-700 transition disabled:opacity-60">
                {runningAtp ? <Loader2 size={14} className="animate-spin" /> : <Gauge size={14} />}
                Evaluate Available-to-Promise
              </button>
              {atpResults && (
                <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
                  {atpResults.map(r => (
                    <div key={r.salesOrderLineId} className="p-4 flex items-center justify-between text-xs">
                      <div>
                        <div className="font-bold text-slate-800">{r.partNumber || `Part #${r.partId}`}</div>
                        <div className="text-slate-400">Requested {r.requestedQuantity} · Available now {r.availableNow}</div>
                      </div>
                      <div className="text-right">
                        <span className={`px-2.5 py-1 rounded-full text-[10px] font-black border ${
                          r.canFulfillNow ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}>
                          {r.canFulfillNow ? 'CAN FULFILL NOW' : `SHORT ${r.shortageQuantity}`}
                        </span>
                        <div className="text-slate-400 mt-1">Earliest: {r.earliestDeliveryDate}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Fulfillment: allocation summary, dispatch pick/pack/confirm, tracking */}
          {activeTab === 'fulfillment' && (
            <div className="space-y-6">
              {tracking && tracking.allocations.length > 0 && (
                <div>
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Allocations</p>
                  <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
                    {tracking.allocations.map((a, i) => (
                      <div key={i} className="p-3 flex items-center justify-between text-xs">
                        <span className="font-bold text-slate-700">{a.partNumber} · batch {a.batchNumber}</span>
                        <span className="text-slate-500">{a.quantity} units</span>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-black border ${
                          a.status === 'SHIPPED' ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                          : a.status === 'ACTIVE' ? 'bg-violet-50 text-violet-700 border-violet-200'
                          : 'bg-slate-100 text-slate-500 border-slate-200'
                        }`}>{a.status}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {order.status === 'ALLOCATED' && canFulfill && dispatches.length === 0 && (
                <button onClick={createPickList} disabled={acting}
                  className="flex items-center gap-2 px-4 py-2.5 bg-orange-500 text-white text-xs font-black rounded-xl hover:bg-orange-600 transition disabled:opacity-60">
                  <PlusCircle size={14} /> Create Pick List
                </button>
              )}

              {dispatches.map(d => (
                <div key={d.id} className="border border-slate-100 rounded-2xl overflow-hidden">
                  <div className="p-4 bg-slate-50 flex items-center justify-between">
                    <span className="font-mono font-black text-slate-800 text-sm">{d.dispatchNumber}</span>
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-black border bg-white text-slate-600 border-slate-200">{d.status}</span>
                  </div>
                  <div className="p-4 space-y-2">
                    {d.lines.map(l => (
                      <div key={l.id} className="flex items-center justify-between text-xs">
                        <span className="text-slate-700 font-bold">{l.partNumber || `Part #${l.partId}`}</span>
                        <span className="text-slate-400">{l.quantity} units</span>
                      </div>
                    ))}
                    {d.status === 'DISPATCHED' && (
                      <div className="text-xs text-slate-500 pt-2 border-t border-slate-100 mt-2">
                        {d.carrierName} · Tracking {d.trackingNumber} · {d.dispatchedAt ? new Date(d.dispatchedAt).toLocaleString() : ''}
                      </div>
                    )}
                    {canFulfill && d.status === 'PENDING' && (
                      <button onClick={() => markPacked(d.id)} disabled={acting}
                        className="mt-2 flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-60">
                        <PackageCheck size={12} /> Mark Packed
                      </button>
                    )}
                    {canFulfill && d.status === 'PACKED' && (
                      confirmingDispatchId === d.id ? (
                        <div className="mt-2 flex flex-wrap gap-2 items-center">
                          <input value={carrierName} onChange={e => setCarrierName(e.target.value)} placeholder="Carrier name"
                            className="px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-[11px] w-32" />
                          <input value={trackingNumber} onChange={e => setTrackingNumber(e.target.value)} placeholder="Tracking #"
                            className="px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-[11px] w-32" />
                          <button onClick={() => confirmDispatch(d.id)} disabled={acting}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60">
                            <Truck size={12} /> Confirm
                          </button>
                        </div>
                      ) : (
                        <button onClick={() => setConfirmingDispatchId(d.id)}
                          className="mt-2 flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black bg-indigo-600 text-white hover:bg-indigo-700">
                          <Truck size={12} /> Confirm Dispatch
                        </button>
                      )
                    )}
                  </div>
                </div>
              ))}

              {dispatches.length === 0 && (!tracking || tracking.allocations.length === 0) && (
                <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                  Nothing allocated or dispatched yet
                </div>
              )}
            </div>
          )}

          {/* Returns */}
          {activeTab === 'returns' && (
            <div className="space-y-4">
              {canManage && (
                <button onClick={() => setShowReturnForm(!showReturnForm)}
                  className="flex items-center gap-2 px-4 py-2.5 bg-slate-900 text-white text-xs font-black rounded-xl hover:bg-slate-800 transition">
                  <RotateCcw size={14} /> Authorize a Return
                </button>
              )}
              {showReturnForm && (
                <form onSubmit={authorizeReturn} className="p-4 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                  <div className="grid grid-cols-3 gap-2">
                    <select value={returnForm.partId} onChange={e => {
                      const partId = Number(e.target.value);
                      const line = order.lines.find(l => l.partId === partId);
                      setReturnForm({ ...returnForm, partId, partNumber: line?.partNumber || '' });
                    }} className="px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs">
                      <option value={0}>— Select part —</option>
                      {order.lines.map(l => <option key={l.id} value={l.partId}>{l.partNumber || l.partName}</option>)}
                    </select>
                    <input type="number" min={1} value={returnForm.quantity}
                      onChange={e => setReturnForm({ ...returnForm, quantity: Number(e.target.value) })}
                      placeholder="Quantity" className="px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                    <input value={returnForm.reasonCode} onChange={e => setReturnForm({ ...returnForm, reasonCode: e.target.value })}
                      placeholder="Reason code" className="px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  </div>
                  <textarea value={returnForm.reasonDescription} onChange={e => setReturnForm({ ...returnForm, reasonDescription: e.target.value })}
                    placeholder="Reason description" rows={2}
                    className="w-full px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs resize-none" />
                  <button type="submit" disabled={acting}
                    className="w-full py-2 rounded-lg font-black text-white bg-slate-900 hover:bg-slate-800 text-xs disabled:opacity-60">
                    Authorize Return
                  </button>
                </form>
              )}

              <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
                {returns.length === 0 ? (
                  <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                    No returns for this order
                  </div>
                ) : returns.map(r => (
                  <div key={r.id} className="p-4 flex items-center justify-between text-xs">
                    <div>
                      <div className="font-mono font-black text-slate-800">{r.rmaNumber}</div>
                      <div className="text-slate-400">{r.partNumber} × {r.quantity}{r.reasonCode ? ` · ${r.reasonCode}` : ''}</div>
                    </div>
                    <span className={`px-2.5 py-1 rounded-full text-[10px] font-black border ${
                      r.disposition === 'ACCEPTED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : r.disposition === 'REJECTED' ? 'bg-red-50 text-red-700 border-red-200'
                      : r.disposition === 'PENDING_INSPECTION' ? 'bg-amber-50 text-amber-700 border-amber-200'
                      : 'bg-slate-100 text-slate-500 border-slate-200'
                    }`}>{r.disposition.replace('_', ' ')}</span>
                  </div>
                ))}
              </div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">
                Receiving and inspecting a return happens on the Returns page.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
