import React, { useEffect, useState } from 'react';
import salesApi from '../api/salesAxios';
import toast from 'react-hot-toast';
import { Users, Loader2, ChevronRight, CheckCircle2, PlusCircle } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Customer, CustomerRequest } from '../types/sales';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_MANAGE_SALES } from '../utils/permissions';

const statusStyle = (status: string) => {
  if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (status === 'ON_HOLD') return 'bg-amber-50 text-amber-700 border-amber-200';
  return 'bg-slate-100 text-slate-500 border-slate-200'; // INACTIVE
};

const emptyForm: CustomerRequest = {
  name: '', contactPerson: '', email: '', phone: '',
  billingAddress: '', shippingAddress: '', currency: '', paymentTerms: '', status: 'ACTIVE',
};

const CustomersPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_SALES);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Customer | null>(null);
  const [showRegister, setShowRegister] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState<CustomerRequest>(emptyForm);
  const [registerForm, setRegisterForm] = useState<CustomerRequest>(emptyForm);

  const fetchCustomers = async () => {
    setLoading(true);
    try {
      const res = await salesApi.get<ApiResponse<Customer[]>>('/sales/customers');
      setCustomers(res.data.data);
    } catch {
      toast.error('Failed to fetch customers.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCustomers(); }, []);

  const handleSelect = (c: Customer) => {
    setSelected(c);
    setForm({
      name: c.name, contactPerson: c.contactPerson || '', email: c.email || '', phone: c.phone || '',
      billingAddress: c.billingAddress || '', shippingAddress: c.shippingAddress || '',
      currency: c.currency || '', paymentTerms: c.paymentTerms || '', status: c.status,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      await toast.promise(
        salesApi.put(`/sales/customers/${selected.id}`, form),
        {
          loading: 'Updating customer...',
          success: `Customer ${selected.customerCode} updated.`,
          error: (err) => err.response?.data?.message || 'Failed to update customer.',
        }
      );
      await fetchCustomers();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!registerForm.name.trim()) { toast.error('Name is required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        salesApi.post('/sales/customers', registerForm),
        {
          loading: 'Registering customer...',
          success: 'Customer registered.',
          error: (err) => err.response?.data?.message || 'Failed to register customer.',
        }
      );
      setShowRegister(false);
      setRegisterForm(emptyForm);
      await fetchCustomers();
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
            <Users size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Customers</h1>
            <p className="text-slate-500 text-sm font-medium">
              Customer master data — the foundation every Quotation and Sales Order points at.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowRegister(!showRegister); setRegisterForm(emptyForm); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> Register Customer
          </button>
        )}
      </div>

      {showRegister && (
        <form onSubmit={handleRegister} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Customer</h2>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Name</label>
              <input value={registerForm.name} onChange={e => setRegisterForm({ ...registerForm, name: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Contact Person</label>
              <input value={registerForm.contactPerson} onChange={e => setRegisterForm({ ...registerForm, contactPerson: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Email</label>
              <input value={registerForm.email} onChange={e => setRegisterForm({ ...registerForm, email: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Phone</label>
              <input value={registerForm.phone} onChange={e => setRegisterForm({ ...registerForm, phone: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Billing Address</label>
              <input value={registerForm.billingAddress} onChange={e => setRegisterForm({ ...registerForm, billingAddress: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Shipping Address</label>
              <input value={registerForm.shippingAddress} onChange={e => setRegisterForm({ ...registerForm, shippingAddress: e.target.value })}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Currency</label>
              <input value={registerForm.currency} onChange={e => setRegisterForm({ ...registerForm, currency: e.target.value })}
                placeholder="USD" className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Payment Terms</label>
              <input value={registerForm.paymentTerms} onChange={e => setRegisterForm({ ...registerForm, paymentTerms: e.target.value })}
                placeholder="Net 30" className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowRegister(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Register
            </button>
          </div>
        </form>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Customer list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100">
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Customer Register</h2>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : customers.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No customers registered yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {customers.map(c => (
                <button key={c.id} onClick={() => handleSelect(c)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === c.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">{c.name}</div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {c.customerCode}{c.contactPerson ? ` · ${c.contactPerson}` : ''}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </div>
                  <div className="flex flex-wrap gap-1 mt-2">
                    <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle(c.status)}`}>{c.status}</span>
                    {c.paymentTerms && (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-sky-50 text-sky-700 border-sky-200">{c.paymentTerms}</span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Edit customer */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <Users size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Edit Customer</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <Users size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select a customer to edit</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="font-black text-slate-900">{selected.customerCode}</div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Name</label>
                <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} disabled={!canManage}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Contact Person</label>
                  <input value={form.contactPerson} onChange={e => setForm({ ...form, contactPerson: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Email</label>
                  <input value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Billing Address</label>
                  <input value={form.billingAddress} onChange={e => setForm({ ...form, billingAddress: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Shipping Address</label>
                  <input value={form.shippingAddress} onChange={e => setForm({ ...form, shippingAddress: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Currency</label>
                  <input value={form.currency} onChange={e => setForm({ ...form, currency: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Payment Terms</label>
                  <input value={form.paymentTerms} onChange={e => setForm({ ...form, paymentTerms: e.target.value })} disabled={!canManage}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60" />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Status</label>
                <div className="grid grid-cols-3 gap-2">
                  {['ACTIVE', 'ON_HOLD', 'INACTIVE'].map(s => (
                    <button key={s} type="button" disabled={!canManage} onClick={() => setForm({ ...form, status: s })}
                      className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all disabled:opacity-60 ${
                        form.status === s ? 'bg-slate-900 text-white border-slate-900' : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                      }`}>
                      {s.replace('_', ' ')}
                    </button>
                  ))}
                </div>
              </div>

              {canManage && (
                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Save Changes
                </button>
              )}
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default CustomersPage;
