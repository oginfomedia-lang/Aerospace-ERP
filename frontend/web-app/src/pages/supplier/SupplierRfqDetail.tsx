import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { ArrowLeft, LogOut, CheckCircle2 } from 'lucide-react';
import procurementApi from '../../api/procurementAxios';
import { Rfq } from '../../types/procurement';
import { useAuth } from '../../context/AuthContext';

const SupplierRfqDetail: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [rfq, setRfq] = useState<Rfq | null>(null);
  const [unitPrice, setUnitPrice] = useState('');
  const [leadTimeDays, setLeadTimeDays] = useState('');
  const [validityDate, setValidityDate] = useState('');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    procurementApi.get<Rfq>(`/supplier-portal/rfqs/${id}`).then(res => setRfq(res.data)).catch(() => {});
  }, [id]);

  const handleLogout = async () => {
    await logout();
    navigate('/supplier/login');
  };

  const submit = async () => {
    if (!unitPrice || !leadTimeDays) { toast.error('Enter price and lead time'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        procurementApi.post(`/supplier-portal/rfqs/${id}/quote`, {
          unitPrice: Number(unitPrice), leadTimeDays: Number(leadTimeDays),
          validityDate: validityDate || undefined, remarks,
        }),
        { loading: 'Submitting quote...', success: 'Quotation submitted', error: (err) => err.response?.data?.message || 'Failed to submit' }
      );
      setSubmitted(true);
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="bg-slate-900 px-8 py-5 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/supplier/dashboard"
            className="flex items-center gap-1.5 text-slate-400 hover:text-white text-xs font-bold transition">
            <ArrowLeft size={14} /> Dashboard
          </Link>
          <div>
            <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Supplier Portal</p>
            <h1 className="text-lg font-black text-white">{user?.username || 'Supplier'}</h1>
          </div>
        </div>
        <button onClick={handleLogout}
          className="flex items-center gap-2 px-4 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl text-xs font-bold transition">
          <LogOut size={14} /> Logout
        </button>
      </div>

      {!rfq ? (
        <div className="p-8 text-slate-400 text-sm">Loading...</div>
      ) : (
        <div className="p-8 max-w-lg space-y-4">
          <h1 className="text-xl font-black text-slate-900">{rfq.rfqNumber} — {rfq.partName}</h1>
          <p className="text-xs text-slate-500">Quantity requested: {rfq.quantity} · Quote by {new Date(rfq.dueDate).toLocaleString()}</p>
          {rfq.specification && <p className="text-sm text-slate-600 bg-slate-50 p-3 rounded-xl">{rfq.specification}</p>}

          {submitted ? (
            <div className="bg-white p-6 rounded-2xl border border-emerald-200 space-y-3 text-center">
              <CheckCircle2 className="mx-auto text-emerald-500" size={40} />
              <p className="font-black text-slate-900">Quotation Submitted</p>
              <p className="text-xs text-slate-500">
                ₹{unitPrice} / unit · {leadTimeDays} day lead time. You'll be notified once the buyer decides.
              </p>
              <Link to="/supplier/dashboard"
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-slate-900 text-white font-black rounded-xl text-sm">
                Back to Dashboard
              </Link>
            </div>
          ) : (
            <div className="bg-white p-5 rounded-2xl border border-slate-100 space-y-3">
              <div>
                <label className="text-xs font-black text-slate-500 uppercase block mb-1">Unit Price</label>
                <input type="number" value={unitPrice} onChange={e => setUnitPrice(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
              </div>
              <div>
                <label className="text-xs font-black text-slate-500 uppercase block mb-1">Lead Time (days)</label>
                <input type="number" value={leadTimeDays} onChange={e => setLeadTimeDays(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
              </div>
              <div>
                <label className="text-xs font-black text-slate-500 uppercase block mb-1">Quote Valid Until</label>
                <input type="date" value={validityDate} onChange={e => setValidityDate(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
              </div>
              <div>
                <label className="text-xs font-black text-slate-500 uppercase block mb-1">Remarks</label>
                <textarea value={remarks} onChange={e => setRemarks(e.target.value)} rows={2}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
              </div>
              <button onClick={submit} disabled={submitting}
                className="w-full py-2.5 bg-indigo-600 text-white font-black rounded-xl disabled:opacity-60">
                Submit Quotation
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default SupplierRfqDetail;
