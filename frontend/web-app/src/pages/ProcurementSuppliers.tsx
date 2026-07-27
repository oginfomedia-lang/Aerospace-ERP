import React, { useState, useEffect } from 'react';
import { Users, Plus, Loader2, RefreshCw, Edit3, Ban, RotateCcw, ChevronDown, ChevronRight, Package, Send, RefreshCcw } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { authApi } from '../api/authAxios';
import api from '../api/axios';
import { Supplier, SupplierRequest } from '../types/procurement';
import { PartBasic } from '../types/inventory';

type PortalStatus = 'NONE' | 'PENDING' | 'ACTIVE';

const emptyForm = {
  name: '',
  vendorCode: '',
  contactPerson: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  state: '',
  stateCode: '',
  pincode: '',
  country: '',
  gstin: '',
  pan: '',
  gstinRegType: '',
  certStatus: 'CERTIFIED',
  rating: '',
};

const ProcurementSuppliers = () => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [productsBySupplier, setProductsBySupplier] = useState<Record<number, PartBasic[]>>({});
  const [portalStatus, setPortalStatus] = useState<Record<number, PortalStatus>>({});

  // Form State
  const [form, setForm] = useState(emptyForm);

  const fetchInviteStatuses = async (list: Supplier[]) => {
    if (list.length === 0) return;
    try {
      const res = await authApi.get<Record<string, PortalStatus>>('/suppliers/invite-status', {
        params: { supplierIds: list.map(s => s.id).join(',') },
      });
      const byNumber: Record<number, PortalStatus> = {};
      Object.entries(res.data).forEach(([id, status]) => { byNumber[Number(id)] = status; });
      setPortalStatus(byNumber);
    } catch {
      // non-critical — badges just fall back to "Not Invited"
    }
  };

  const fetchSuppliers = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get('/suppliers');
      setSuppliers(res.data);
      fetchInviteStatuses(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleOpenCreate = () => {
    setEditingSupplier(null);
    setForm(emptyForm);
    setShowModal(true);
  };

  const handleOpenEdit = (supplier: Supplier) => {
    setEditingSupplier(supplier);
    setForm({
      name: supplier.name,
      vendorCode: supplier.vendorCode,
      contactPerson: supplier.contactPerson || '',
      email: supplier.email || '',
      phone: supplier.phone || '',
      address: supplier.address || '',
      city: supplier.city || '',
      state: supplier.state || '',
      stateCode: supplier.stateCode || '',
      pincode: supplier.pincode || '',
      country: supplier.country || '',
      gstin: supplier.gstin || '',
      pan: supplier.pan || '',
      gstinRegType: supplier.gstinRegType || '',
      certStatus: supplier.certificationStatus,
      rating: supplier.rating || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: SupplierRequest = {
      name: form.name,
      vendorCode: form.vendorCode,
      contactPerson: form.contactPerson,
      email: form.email,
      phone: form.phone,
      address: form.address,
      city: form.city || undefined,
      state: form.state || undefined,
      stateCode: form.stateCode || undefined,
      pincode: form.pincode || undefined,
      country: form.country || undefined,
      gstin: form.gstin || undefined,
      pan: form.pan || undefined,
      gstinRegType: form.gstinRegType || undefined,
      certificationStatus: form.certStatus,
      rating: form.rating || undefined,
    };

    try {
      if (editingSupplier) {
        const res = await procurementApi.put(`/suppliers/${editingSupplier.id}`, payload);
        toast.success('Supplier updated successfully!');
        setSuppliers(suppliers.map(s => s.id === editingSupplier.id ? res.data : s));
      } else {
        const res = await procurementApi.post('/suppliers', payload);
        toast.success('Supplier registered successfully!');
        setSuppliers([...suppliers, res.data]);
      }
      setShowModal(false);
      setEditingSupplier(null);
      setForm(emptyForm);
    } catch (err) {
      console.error(err);
    }
  };

  const handleToggleActive = async (supplier: Supplier) => {
    const action = supplier.active ? 'deactivate' : 'reactivate';
    const confirmed = window.confirm(
      supplier.active
        ? `Deactivate ${supplier.name}? Existing POs are unaffected, but no new POs can be created for them until reactivated.`
        : `Reactivate ${supplier.name}?`
    );
    if (!confirmed) return;

    try {
      const res = supplier.active
        ? await procurementApi.delete(`/suppliers/${supplier.id}`)
        : await procurementApi.patch(`/suppliers/${supplier.id}/reactivate`);
      toast.success(`Supplier ${action}d.`);
      setSuppliers(suppliers.map(s => s.id === supplier.id ? res.data : s));
    } catch (err) {
      console.error(err);
    }
  };

  const handleInviteToPortal = async (supplier: Supplier) => {
    if (!supplier.email) {
      toast.error('This supplier has no email on file — add one via Edit first.');
      return;
    }
    const confirmed = window.confirm(
      `Invite ${supplier.name} to the Supplier Portal? An activation email will be sent to ${supplier.email}.`
    );
    if (!confirmed) return;

    try {
      await authApi.post('/suppliers/invite', {
        supplierId: supplier.id,
        email: supplier.email,
        supplierName: supplier.name,
      });
      toast.success(`Invitation sent to ${supplier.email}`);
      setPortalStatus(prev => ({ ...prev, [supplier.id]: 'PENDING' }));
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to send invite');
    }
  };

  const handleResendInvite = async (supplier: Supplier) => {
    const confirmed = window.confirm(
      `Resend the activation email to ${supplier.email}? The previous activation link will still work, but this issues a fresh one valid for 7 more days.`
    );
    if (!confirmed) return;

    try {
      await authApi.post('/suppliers/resend-invite', {
        email: supplier.email,
        supplierName: supplier.name,
      });
      toast.success(`Invitation resent to ${supplier.email}`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to resend invite');
    }
  };

  const handleToggleExpand = (supplierId: number) => {
    if (expandedId === supplierId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(supplierId);
    if (!productsBySupplier[supplierId]) {
      api.get(`/inventory/suppliers/${supplierId}/parts`)
        .then(res => setProductsBySupplier(prev => ({ ...prev, [supplierId]: res.data.data })))
        .catch(() => setProductsBySupplier(prev => ({ ...prev, [supplierId]: [] })));
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Suppliers</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">Manage aerospace vendor network</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchSuppliers}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
          >
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20"
          >
            <Plus size={18} />
            Add Supplier
          </button>
        </div>
      </div>

      {loading && suppliers.length === 0 ? (
        <div className="flex items-center justify-center p-12">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
              <tr>
                <th className="px-6 py-4">Vendor</th>
                <th className="px-6 py-4">Code</th>
                <th className="px-6 py-4">Contact</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Rating</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {suppliers.map(supplier => {
                const isExpanded = expandedId === supplier.id;
                const products = productsBySupplier[supplier.id];
                return (
                <React.Fragment key={supplier.id}>
                <tr className={`hover:bg-slate-50/50 transition-colors ${!supplier.active ? 'opacity-60' : ''}`}>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <button onClick={() => handleToggleExpand(supplier.id)} title="Products supplied"
                        className="p-0.5 text-slate-400 hover:text-slate-700">
                        {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                      </button>
                      <div className="font-bold text-slate-900">{supplier.name}</div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-slate-100 text-slate-600 font-mono text-xs rounded-md border border-slate-200">
                      {supplier.vendorCode}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-slate-900">{supplier.email}</div>
                    <div className="text-slate-500 text-xs mt-0.5">{supplier.phone}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex flex-wrap gap-1.5">
                      <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                        supplier.certificationStatus === 'CERTIFIED' ? 'bg-emerald-100 text-emerald-700' :
                        supplier.certificationStatus === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                        'bg-red-100 text-red-700'
                      }`}>
                        {supplier.certificationStatus}
                      </span>
                      {!supplier.active && (
                        <span className="px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full bg-slate-200 text-slate-600">
                          INACTIVE
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right font-medium text-slate-900">
                    {supplier.rating || '—'}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end items-center gap-1">
                      {(() => {
                        const status = portalStatus[supplier.id] || 'NONE';
                        if (status === 'ACTIVE') {
                          return (
                            <span className="mr-1 px-2.5 py-1 text-[10px] font-black tracking-wider uppercase rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                              Portal Active
                            </span>
                          );
                        }
                        if (status === 'PENDING') {
                          return (
                            <>
                              <span className="mr-1 px-2.5 py-1 text-[10px] font-black tracking-wider uppercase rounded-full bg-amber-50 text-amber-700 border border-amber-200">
                                Invite Pending
                              </span>
                              <button
                                onClick={() => handleResendInvite(supplier)}
                                title="Resend activation email"
                                className="p-2 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-xl transition"
                              >
                                <RefreshCcw size={16} />
                              </button>
                            </>
                          );
                        }
                        return (
                          <button
                            onClick={() => handleInviteToPortal(supplier)}
                            title="Invite to Supplier Portal"
                            className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-xl transition"
                          >
                            <Send size={16} />
                          </button>
                        );
                      })()}
                      <button
                        onClick={() => handleOpenEdit(supplier)}
                        title="Edit supplier"
                        className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition"
                      >
                        <Edit3 size={16} />
                      </button>
                      <button
                        onClick={() => handleToggleActive(supplier)}
                        title={supplier.active ? 'Deactivate supplier' : 'Reactivate supplier'}
                        className={`p-2 rounded-xl transition ${supplier.active ? 'text-slate-400 hover:text-red-600 hover:bg-red-50' : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50'}`}
                      >
                        {supplier.active ? <Ban size={16} /> : <RotateCcw size={16} />}
                      </button>
                    </div>
                  </td>
                </tr>
                {isExpanded && (
                  <tr className="bg-slate-50/70">
                    <td colSpan={6} className="px-6 py-4">
                      <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mb-2 flex items-center gap-1.5">
                        <Package size={12} /> Products Supplied
                      </p>
                      {products === undefined ? (
                        <Loader2 size={16} className="animate-spin text-slate-400" />
                      ) : products.length === 0 ? (
                        <p className="text-xs text-slate-400">Not marked as an approved supplier for any part yet.</p>
                      ) : (
                        <div className="flex flex-wrap gap-2">
                          {products.map(p => (
                            <span key={p.id}
                              className={`px-2.5 py-1 text-xs font-bold rounded-lg border ${p.preferred ? 'bg-amber-50 border-amber-200 text-amber-700' : 'bg-white border-slate-200 text-slate-600'}`}>
                              {p.name} ({p.skuCode}){p.preferred ? ' ★' : ''}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                  </tr>
                )}
                </React.Fragment>
                );
              })}
              {suppliers.length === 0 && !loading && (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-slate-500">
                    <Users className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                    <p className="font-medium">No suppliers found</p>
                    <p className="text-sm mt-1">Register a new vendor to get started.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl animate-in fade-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-slate-100 shrink-0">
              <h2 className="text-xl font-bold text-slate-900">
                {editingSupplier ? `Edit Supplier: ${editingSupplier.name}` : 'Register New Supplier'}
              </h2>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
            <div className="p-6 space-y-4 overflow-y-auto flex-1">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Company Name</label>
                <input required type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="Boeing Aerospace" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Vendor Code</label>
                <input required type="text" value={form.vendorCode} onChange={e => setForm({ ...form, vendorCode: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono"
                  placeholder="BOE-9901" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Contact Person</label>
                <input type="text" value={form.contactPerson} onChange={e => setForm({ ...form, contactPerson: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="Jane Doe" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Email</label>
                  <input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="sales@boeing.com" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Phone</label>
                  <input required type="text" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="+1 800 555 0199" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Address</label>
                <input type="text" value={form.address} onChange={e => setForm({ ...form, address: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="123 Aerospace Way, Seattle, WA" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">City</label>
                  <input type="text" value={form.city} onChange={e => setForm({ ...form, city: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Pincode</label>
                  <input type="text" value={form.pincode} onChange={e => setForm({ ...form, pincode: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">State</label>
                  <input type="text" value={form.state} onChange={e => setForm({ ...form, state: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">State Code</label>
                  <input type="text" value={form.stateCode} onChange={e => setForm({ ...form, stateCode: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="e.g. 22" />
                  <p className="text-[10px] text-slate-400 mt-1">Used to decide CGST+SGST vs IGST on POs.</p>
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Country</label>
                  <input type="text" value={form.country} onChange={e => setForm({ ...form, country: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                </div>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">GSTIN No.</label>
                  <input type="text" value={form.gstin} onChange={e => setForm({ ...form, gstin: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">PAN No.</label>
                  <input type="text" value={form.pan} onChange={e => setForm({ ...form, pan: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">GSTIN Reg. Type</label>
                  <select value={form.gstinRegType} onChange={e => setForm({ ...form, gstinRegType: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                    <option value="">—</option>
                    <option value="Full GST Registered">Full GST Registered</option>
                    <option value="Composition">Composition</option>
                    <option value="Unregistered">Unregistered</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Certification Status</label>
                  <select value={form.certStatus} onChange={e => setForm({ ...form, certStatus: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                    <option value="CERTIFIED">CERTIFIED</option>
                    <option value="PENDING">PENDING</option>
                    <option value="REVOKED">REVOKED</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Rating</label>
                  <select value={form.rating} onChange={e => setForm({ ...form, rating: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                    <option value="">Unrated</option>
                    <option value="A">A</option>
                    <option value="B">B</option>
                    <option value="C">C</option>
                  </select>
                </div>
              </div>

            </div>
              <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0">
                <button type="button" onClick={() => { setShowModal(false); setEditingSupplier(null); }}
                  className="flex-1 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-all">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
                  {editingSupplier ? 'Save Changes' : 'Save Supplier'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProcurementSuppliers;
