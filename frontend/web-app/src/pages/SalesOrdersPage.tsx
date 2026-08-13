import React, { useEffect, useState } from 'react';
import salesApi from '../api/salesAxios';
import inventoryApi from '../api/axios';
import toast from 'react-hot-toast';
import { ShoppingBag, Loader2, ChevronRight, CheckCircle2, PlusCircle, Plus, Trash2, Boxes, PackageX, Clock } from 'lucide-react';
import { ApiResponse, Part } from '../types/inventory';
import { Customer, LineItemRequest, SalesOrder, SalesOrderRequest, SalesReportSummary } from '../types/sales';
import { SalesOrderDetailModal } from '../components/SalesOrderDetailModal';
import { PartPicker } from '../components/PartPicker';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_MANAGE_SALES, CAN_APPROVE_SALES } from '../utils/permissions';

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

const emptyLine: LineItemRequest = { partId: 0, partNumber: '', partName: '', quantity: 1, unitPrice: 0, discountPercent: 0, taxPercent: 0 };

const emptyForm = (customerId = 0): SalesOrderRequest => ({
  customerId, orderType: 'STANDARD', requestedDeliveryDate: '', shippingAddress: '', priority: 'MEDIUM',
  lines: [{ ...emptyLine }],
});

const SalesOrdersPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_SALES);
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_SALES);
  const [summary, setSummary] = useState<SalesReportSummary | null>(null);
  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<SalesOrder | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<SalesOrderRequest>(emptyForm());
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!canApprove) return;
    salesApi.get<ApiResponse<SalesReportSummary>>('/sales/reports/summary', { params: { days: 30 } })
      .then(res => setSummary(res.data.data)).catch(() => {});
  }, [canApprove]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const res = await salesApi.get<ApiResponse<SalesOrder[]>>('/sales/orders');
      setOrders(res.data.data);
      if (selected) {
        const fresh = res.data.data.find(o => o.id === selected.id);
        setSelected(fresh || null);
      }
    } catch {
      toast.error('Failed to fetch sales orders.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOrders(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    salesApi.get<ApiResponse<Customer[]>>('/sales/customers')
      .then(res => setCustomers(res.data.data)).catch(() => {});
    inventoryApi.get<ApiResponse<Part[]>>('/inventory')
      .then(res => setParts(res.data.data)).catch(() => {});
  }, []);

  const updateLine = (idx: number, patch: Partial<LineItemRequest>) => {
    setForm(prev => ({ ...prev, lines: prev.lines.map((l, i) => i === idx ? { ...l, ...patch } : l) }));
  };
  const addLine = () => setForm(prev => ({ ...prev, lines: [...prev.lines, { ...emptyLine }] }));
  const removeLine = (idx: number) => setForm(prev => ({ ...prev, lines: prev.lines.filter((_, i) => i !== idx) }));

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.customerId) { toast.error('Select a customer first.'); return; }
    if (form.lines.some(l => !l.partId || !l.quantity || !l.unitPrice)) {
      toast.error('Every line needs a part, quantity, and unit price.'); return;
    }
    setSubmitting(true);
    try {
      await toast.promise(
        salesApi.post('/sales/orders', form),
        { loading: 'Creating sales order...', success: 'Sales order created.', error: (err) => err.response?.data?.message || 'Failed to create sales order.' }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchOrders();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <ShoppingBag size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Sales Orders</h1>
            <p className="text-slate-500 text-sm font-medium">
              The real commitment — approval, ATP, allocation, dispatch, and returns all happen here.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Sales Order
          </button>
        )}
      </div>

      {canApprove && summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Orders (30d)', value: summary.orderIntakeCount, icon: <ShoppingBag size={18} />, color: 'from-blue-600 to-blue-700' },
            { label: 'Open Orders', value: summary.openOrdersCount, icon: <Boxes size={18} />, color: 'from-violet-600 to-violet-700' },
            { label: 'Backordered', value: summary.backorderedOrdersCount, icon: <PackageX size={18} />, color: 'from-amber-500 to-amber-600' },
            { label: 'On-Time Delivery', value: `${summary.onTimeDeliveryPercent}%`, icon: <Clock size={18} />, color: 'from-emerald-600 to-emerald-700' },
          ].map(card => (
            <div key={card.label} className="bg-white rounded-3xl p-5 shadow-sm border border-slate-100">
              <div className={`inline-flex p-3 rounded-2xl bg-gradient-to-br ${card.color} text-white shadow-lg`}>{card.icon}</div>
              <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.15em] mt-4 mb-1">{card.label}</p>
              <p className="text-2xl font-black text-slate-900 tracking-tight">{card.value}</p>
            </div>
          ))}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Sales Order</h2>

          <div className="grid grid-cols-4 gap-3">
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Customer</label>
              <select value={form.customerId} onChange={e => setForm({ ...form, customerId: Number(e.target.value) })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value={0}>— Select —</option>
                {customers.map(c => <option key={c.id} value={c.id}>{c.name} ({c.customerCode})</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Order Type</label>
              <input value={form.orderType} onChange={e => setForm({ ...form, orderType: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Priority</label>
              <select value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map(p => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Requested Delivery Date</label>
              <input type="date" value={form.requestedDeliveryDate} onChange={e => setForm({ ...form, requestedDeliveryDate: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Shipping Address</label>
              <input value={form.shippingAddress} onChange={e => setForm({ ...form, shippingAddress: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Line Items</label>
              <button type="button" onClick={addLine}
                className="flex items-center gap-1 text-[10px] font-black px-3 py-1 rounded-full bg-slate-900 text-white">
                <Plus size={12} /> Add Line
              </button>
            </div>
            <div className="space-y-2">
              {form.lines.map((l, i) => (
                <div key={i} className="grid grid-cols-12 gap-2 items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="col-span-6">
                    <PartPicker parts={parts} value={l.partId} onSelect={part => updateLine(i, {
                      partId: part.id, partNumber: part.serialNumber, partName: part.name,
                      unitPrice: l.unitPrice || part.unitPrice || 0,
                    })} />
                  </div>
                  <input type="number" min={1} placeholder="Qty" value={l.quantity} onChange={e => updateLine(i, { quantity: Number(e.target.value) })}
                    className="col-span-1 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  <input type="number" min={0} step="0.01" placeholder="Unit Price" value={l.unitPrice} onChange={e => updateLine(i, { unitPrice: Number(e.target.value) })}
                    className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  <input type="number" min={0} max={100} placeholder="Disc %" value={l.discountPercent} onChange={e => updateLine(i, { discountPercent: Number(e.target.value) })}
                    className="col-span-1 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  <input type="number" min={0} max={100} placeholder="Tax %" value={l.taxPercent} onChange={e => updateLine(i, { taxPercent: Number(e.target.value) })}
                    className="col-span-1 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  <button type="button" onClick={() => removeLine(i)} disabled={form.lines.length === 1}
                    className="col-span-1 flex items-center justify-center text-red-500 disabled:opacity-30">
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Create Sales Order
            </button>
          </div>
        </form>
      )}

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Sales Order Register</h2>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : orders.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No sales orders yet</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {orders.map(o => (
              <button key={o.id} onClick={() => setSelected(o)}
                className="w-full text-left p-5 hover:bg-slate-50 transition-all group flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-black text-slate-900 group-hover:text-blue-600 transition-colors font-mono">{o.orderNumber}</span>
                    {o.revisionNumber > 1 && <span className="text-[10px] text-slate-400 font-bold">Rev {o.revisionNumber}</span>}
                  </div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">{o.customerName}</div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs font-bold text-slate-500">{o.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle[o.status] || ''}`}>{o.status.replace(/_/g, ' ')}</span>
                  <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <SalesOrderDetailModal order={selected} isOpen={!!selected} onClose={() => setSelected(null)} onSuccess={fetchOrders} />
    </div>
  );
};

export default SalesOrdersPage;
