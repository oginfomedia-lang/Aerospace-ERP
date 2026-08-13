import React, { useEffect, useState } from 'react';
import salesApi from '../api/salesAxios';
import inventoryApi from '../api/axios';
import toast from 'react-hot-toast';
import {
  FileText, Loader2, ChevronRight, CheckCircle2, PlusCircle, Plus, Trash2,
  Send, Check, X, ArrowRightCircle,
} from 'lucide-react';
import { ApiResponse, Part } from '../types/inventory';
import { Customer, LineItemRequest, Quotation, QuotationRequest } from '../types/sales';
import { PartPicker } from '../components/PartPicker';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_MANAGE_SALES, CAN_APPROVE_SALES } from '../utils/permissions';

const statusStyle = (status: string) => {
  if (status === 'APPROVED') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (status === 'REJECTED' || status === 'EXPIRED') return 'bg-red-50 text-red-700 border-red-200';
  if (status === 'SUBMITTED') return 'bg-amber-50 text-amber-700 border-amber-200';
  if (status === 'CONVERTED') return 'bg-indigo-50 text-indigo-700 border-indigo-200';
  return 'bg-slate-100 text-slate-500 border-slate-200'; // DRAFT
};

const emptyLine: LineItemRequest = { partId: 0, partNumber: '', partName: '', quantity: 1, unitPrice: 0, discountPercent: 0, taxPercent: 0 };

const emptyForm = (customerId = 0): QuotationRequest => ({
  customerId, validFrom: '', validUntil: '', currency: '', paymentTerms: '', deliveryTerms: '',
  lines: [{ ...emptyLine }],
});

const QuotationsPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_SALES);
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_SALES);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Quotation | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<QuotationRequest>(emptyForm());
  const [submitting, setSubmitting] = useState(false);
  const [actioning, setActioning] = useState(false);

  const fetchQuotations = async () => {
    setLoading(true);
    try {
      const res = await salesApi.get<ApiResponse<Quotation[]>>('/sales/quotations');
      setQuotations(res.data.data);
      if (selected) {
        const fresh = res.data.data.find(q => q.id === selected.id);
        if (fresh) setSelected(fresh);
      }
    } catch {
      toast.error('Failed to fetch quotations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchQuotations(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
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

  const lineTotal = (l: LineItemRequest) => {
    const gross = l.quantity * l.unitPrice;
    const afterDiscount = gross * (1 - (l.discountPercent || 0) / 100);
    return afterDiscount * (1 + (l.taxPercent || 0) / 100);
  };
  const formTotal = form.lines.reduce((sum, l) => sum + lineTotal(l), 0);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.customerId) { toast.error('Select a customer first.'); return; }
    if (form.lines.some(l => !l.partId || !l.quantity || !l.unitPrice)) {
      toast.error('Every line needs a part, quantity, and unit price.'); return;
    }
    setSubmitting(true);
    try {
      await toast.promise(
        salesApi.post('/sales/quotations', form),
        { loading: 'Creating quotation...', success: 'Quotation created.', error: (err) => err.response?.data?.message || 'Failed to create quotation.' }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchQuotations();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const runAction = async (action: 'submit' | 'approve' | 'reject' | 'convert') => {
    if (!selected) return;
    setActioning(true);
    try {
      await toast.promise(
        salesApi.post(`/sales/quotations/${selected.id}/${action}`),
        {
          loading: `${action === 'convert' ? 'Converting' : action[0].toUpperCase() + action.slice(1) + 'ing'}...`,
          success: action === 'convert' ? 'Converted to Sales Order.' : `Quotation ${action}${action.endsWith('e') ? 'd' : 'ed'}.`,
          error: (err) => err.response?.data?.message || `Failed to ${action}.`,
        }
      );
      await fetchQuotations();
    } catch {
      // toast.promise handles display
    } finally {
      setActioning(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <FileText size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Quotations</h1>
            <p className="text-slate-500 text-sm font-medium">
              Customer proposals — nothing is reserved until one converts to a Sales Order.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Quotation
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Quotation</h2>

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
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Valid From</label>
              <input type="date" value={form.validFrom} onChange={e => setForm({ ...form, validFrom: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Valid Until</label>
              <input type="date" value={form.validUntil} onChange={e => setForm({ ...form, validUntil: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Currency</label>
              <input value={form.currency} onChange={e => setForm({ ...form, currency: e.target.value })} placeholder="USD"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Payment Terms</label>
              <input value={form.paymentTerms} onChange={e => setForm({ ...form, paymentTerms: e.target.value })} placeholder="Net 30"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Delivery Terms</label>
              <input value={form.deliveryTerms} onChange={e => setForm({ ...form, deliveryTerms: e.target.value })} placeholder="FOB origin"
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
            <div className="text-right mt-2 text-sm font-black text-slate-700">
              Total: {formTotal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Create Quotation
            </button>
          </div>
        </form>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Quotation list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100">
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Quotation Register</h2>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : quotations.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No quotations yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50 max-h-[70vh] overflow-y-auto">
              {quotations.map(q => (
                <button key={q.id} onClick={() => setSelected(q)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === q.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors font-mono">{q.quotationNumber}</div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">{q.customerName}</div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </div>
                  <div className="flex flex-wrap gap-1 mt-2 items-center">
                    <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle(q.status)}`}>{q.status}</span>
                    <span className="text-xs font-bold text-slate-500">
                      {q.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })} {q.currency || ''}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Detail + actions */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <FileText size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Quotation Detail</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <FileText size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select a quotation</p>
            </div>
          ) : (
            <div className="p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-mono font-black text-slate-900 text-lg">{selected.quotationNumber}</div>
                  <div className="text-xs text-slate-500 font-bold">{selected.customerName}</div>
                </div>
                <span className={`text-[10px] font-black px-3 py-1 rounded-full border ${statusStyle(selected.status)}`}>{selected.status}</span>
              </div>

              <div className="divide-y divide-slate-50 border border-slate-100 rounded-2xl overflow-hidden">
                {selected.lines.map(l => (
                  <div key={l.id} className="p-3 flex items-center justify-between text-xs">
                    <div>
                      <div className="font-bold text-slate-800">{l.partName || l.partNumber || `Part #${l.partId}`}</div>
                      <div className="text-slate-400">{l.quantity} × {l.unitPrice.toFixed(2)}
                        {l.discountPercent ? ` · -${l.discountPercent}%` : ''}{l.taxPercent ? ` · +${l.taxPercent}% tax` : ''}</div>
                    </div>
                    <div className="font-black text-slate-700">{l.lineTotal.toFixed(2)}</div>
                  </div>
                ))}
              </div>

              <div className="flex items-center justify-between p-3 bg-slate-50 rounded-xl">
                <span className="text-xs font-black text-slate-500 uppercase">Total</span>
                <span className="text-lg font-black text-slate-900">
                  {selected.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })} {selected.currency || ''}
                </span>
              </div>

              {selected.convertedToSalesOrderId && (
                <div className="text-xs font-bold text-indigo-600 bg-indigo-50 border border-indigo-100 rounded-xl p-3">
                  Converted to Sales Order #{selected.convertedToSalesOrderId}
                </div>
              )}

              <div className="flex flex-wrap gap-2 pt-2">
                {selected.status === 'DRAFT' && canManage && (
                  <button onClick={() => runAction('submit')} disabled={actioning}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black bg-blue-600 hover:bg-blue-700 text-white shadow-lg shadow-blue-200 disabled:opacity-60">
                    <Send size={13} /> Submit for Approval
                  </button>
                )}
                {selected.status === 'SUBMITTED' && canApprove && (
                  <>
                    <button onClick={() => runAction('approve')} disabled={actioning}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-200 disabled:opacity-60">
                      <Check size={13} /> Approve
                    </button>
                    <button onClick={() => runAction('reject')} disabled={actioning}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-200 disabled:opacity-60">
                      <X size={13} /> Reject
                    </button>
                  </>
                )}
                {selected.status === 'APPROVED' && canManage && (
                  <button onClick={() => runAction('convert')} disabled={actioning}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-200 disabled:opacity-60">
                    <ArrowRightCircle size={13} /> Convert to Sales Order
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default QuotationsPage;
