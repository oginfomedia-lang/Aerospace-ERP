import React, { useState, useEffect } from 'react';
import { Users, Plus, Loader2, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { Supplier, SupplierRequest } from '../types/procurement';

const ProcurementSuppliers = () => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [vendorCode, setVendorCode] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [certStatus, setCertStatus] = useState('CERTIFIED');

  const fetchSuppliers = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get('/suppliers');
      setSuppliers(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: SupplierRequest = {
      name,
      vendorCode,
      contactEmail,
      contactPhone,
      certificationStatus: certStatus,
      rating: 5.0
    };

    try {
      const res = await procurementApi.post('/suppliers', payload);
      toast.success('Supplier registered successfully!');
      setSuppliers([...suppliers, res.data]);
      setShowModal(false);
      // Reset form
      setName('');
      setVendorCode('');
      setContactEmail('');
      setContactPhone('');
      setCertStatus('CERTIFIED');
    } catch (err) {
      console.error(err);
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
            onClick={() => setShowModal(true)}
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
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {suppliers.map(supplier => (
                <tr key={supplier.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="font-bold text-slate-900">{supplier.name}</div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-slate-100 text-slate-600 font-mono text-xs rounded-md border border-slate-200">
                      {supplier.vendorCode}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-slate-900">{supplier.contactEmail}</div>
                    <div className="text-slate-500 text-xs mt-0.5">{supplier.contactPhone}</div>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                      supplier.certificationStatus === 'CERTIFIED' ? 'bg-emerald-100 text-emerald-700' :
                      supplier.certificationStatus === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                      'bg-red-100 text-red-700'
                    }`}>
                      {supplier.certificationStatus}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right font-medium text-slate-900">
                    {supplier.rating !== null && supplier.rating !== undefined ? (
                      typeof supplier.rating === 'number' ? 
                        (supplier.rating as number).toFixed(1) : 
                        (!isNaN(Number(supplier.rating)) ? Number(supplier.rating).toFixed(1) : supplier.rating)
                    ) : '5.0'} / 5.0
                  </td>
                </tr>
              ))}
              {suppliers.length === 0 && !loading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-slate-500">
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
          <div className="bg-white rounded-2xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-xl font-bold text-slate-900">Register New Supplier</h2>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Company Name</label>
                <input required type="text" value={name} onChange={e => setName(e.target.value)} 
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="Boeing Aerospace" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Vendor Code</label>
                <input required type="text" value={vendorCode} onChange={e => setVendorCode(e.target.value)} 
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono"
                  placeholder="BOE-9901" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Email</label>
                  <input required type="email" value={contactEmail} onChange={e => setContactEmail(e.target.value)} 
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="sales@boeing.com" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Phone</label>
                  <input required type="text" value={contactPhone} onChange={e => setContactPhone(e.target.value)} 
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="+1 800 555 0199" />
                </div>
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Certification Status</label>
                <select value={certStatus} onChange={e => setCertStatus(e.target.value)}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                  <option value="CERTIFIED">CERTIFIED</option>
                  <option value="PENDING">PENDING</option>
                  <option value="REVOKED">REVOKED</option>
                </select>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setShowModal(false)}
                  className="flex-1 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-all">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
                  Save Supplier
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
