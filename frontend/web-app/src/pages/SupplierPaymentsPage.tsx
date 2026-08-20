import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { Banknote, Loader2, PlusCircle, XCircle, CheckCircle2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { SupplierPayment, SupplierPaymentCreatePayload, Supplier, PaymentMethod } from '../types/finance';
import { Dropdown } from '../components/Dropdown';
import { fetchSuppliers } from '../api/financeSuppliers';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_MANAGE_FINANCE, CAN_APPROVE_FINANCE } from '../utils/permissions';

const statusStyle: Record<string, string> = {
  DRAFT:     'bg-slate-100  text-slate-600  border-slate-200',
  POSTED:    'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED: 'bg-red-50     text-red-700    border-red-200',
};

const paymentMethods: PaymentMethod[] = ['BANK_TRANSFER', 'CHEQUE', 'CASH', 'ONLINE'];

const emptyForm = (): SupplierPaymentCreatePayload => ({
  invoiceNumber: '',
  supplierId: 0,
  paymentDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  currency: 'INR',
  paymentMethod: 'BANK_TRANSFER',
  remarks: '',
});

const SupplierPaymentsPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_FINANCE);
  const canCancel = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);

  const [payments, setPayments] = useState<SupplierPayment[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [suppliersLoading, setSuppliersLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<SupplierPaymentCreatePayload>(emptyForm());
  const [submitting, setSubmitting] = useState(false);

  const fetchPayments = async () => {
    setLoading(true);
    try {
      const res = await financeApi.get<ApiResponse<SupplierPayment[]>>('/finance/ap/payments');
      setPayments(res.data.data);
    } catch {
      toast.error('Failed to fetch supplier payments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPayments(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetchSuppliers().then(setSuppliers).catch(() => {}).finally(() => setSuppliersLoading(false));
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.invoiceNumber.trim()) { toast.error('Invoice number is required.'); return; }
    if (!form.supplierId) { toast.error('Pick a supplier before recording a payment.'); return; }
    if (!form.amount || form.amount <= 0) { toast.error('Amount must be greater than zero.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/ap/payments', form),
        {
          loading: 'Recording payment...',
          success: 'Supplier payment recorded and posted.',
          error: (err) => err.response?.data?.message || 'Failed to record payment.',
        }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchPayments();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (id: number) => {
    if (!window.confirm('Cancel this payment? The journal entry it posted will be reversed.')) return;
    try {
      await toast.promise(
        financeApi.post(`/finance/ap/payments/${id}/cancel`),
        {
          loading: 'Cancelling payment...',
          success: 'Payment cancelled.',
          error: (err) => err.response?.data?.message || 'Failed to cancel payment.',
        }
      );
      await fetchPayments();
    } catch {
      // toast.promise handles display
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Banknote size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Supplier Payments</h1>
            <p className="text-slate-500 text-sm font-medium">
              Pay down what you owe a supplier — posts Dr Accounts Payable / Cr Bank automatically.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Payment
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Supplier Payment</h2>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Supplier</label>
              <Dropdown<number>
                options={suppliers.map(s => ({ value: s.id, label: s.supplierName, sublabel: s.vendorCode }))}
                value={form.supplierId || ''}
                loading={suppliersLoading}
                placeholder="— Select Supplier —"
                emptyMessage="No suppliers synced yet"
                onChange={(supplierId) => setForm({ ...form, supplierId })}
              />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Invoice Number</label>
              <input value={form.invoiceNumber} onChange={e => setForm({ ...form, invoiceNumber: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Payment Date</label>
              <input type="date" value={form.paymentDate} onChange={e => setForm({ ...form, paymentDate: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>

          <div className="grid grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Amount</label>
              <input type="number" min={0} step="0.01" value={form.amount || ''}
                onChange={e => setForm({ ...form, amount: Number(e.target.value) })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Currency</label>
              <input value={form.currency} maxLength={3} onChange={e => setForm({ ...form, currency: e.target.value.toUpperCase() })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Payment Method</label>
              <select value={form.paymentMethod} onChange={e => setForm({ ...form, paymentMethod: e.target.value as PaymentMethod })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                {paymentMethods.map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Remarks</label>
              <input value={form.remarks} onChange={e => setForm({ ...form, remarks: e.target.value })}
                placeholder="Optional"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Record Payment
            </button>
          </div>
        </form>
      )}

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Payment Register</h2>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : payments.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No supplier payments yet</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {payments.map(p => (
              <div key={p.id} className="p-5 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-black text-slate-900 font-mono">{p.paymentNumber}</span>
                    <span className="text-xs text-slate-400 font-bold">→ {p.supplierName || p.supplierRef}</span>
                  </div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                    {p.paymentDate} · Invoice {p.invoiceNumber} · {p.currency} {p.amount.toLocaleString()} · {p.paymentMethod.replace('_', ' ')}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle[p.status] || ''}`}>{p.status}</span>
                  {canCancel && p.status === 'POSTED' && (
                    <button onClick={() => handleCancel(p.id)}
                      className="flex items-center gap-1 text-xs font-bold text-red-500 hover:text-red-600">
                      <XCircle size={14} /> Cancel
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default SupplierPaymentsPage;
