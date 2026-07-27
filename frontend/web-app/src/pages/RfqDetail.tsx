import React, { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { Rfq, Quotation } from '../types/procurement';
import { ShieldCheck, UserPlus } from 'lucide-react';
import { InviteMoreSuppliersModal } from '../components/InviteMoreSuppliersModal';

const RfqDetail: React.FC = () => {
  const { id } = useParams();
  const [rfq, setRfq] = useState<Rfq | null>(null);
  const [quotes, setQuotes] = useState<Quotation[]>([]);
  const [showInvite, setShowInvite] = useState(false);

  const load = useCallback(() => {
    procurementApi.get<Rfq>(`/rfqs/${id}`).then(res => setRfq(res.data)).catch(() => {});
    procurementApi.get<Quotation[]>(`/rfqs/${id}/quotations`).then(res => setQuotes(res.data)).catch(() => {});
  }, [id]);

  useEffect(load, [load]);

  const selectWinner = async (quotationId: number) => {
    if (!window.confirm('Award this RFQ to this supplier? A real Purchase Order will be created.')) return;
    await toast.promise(
      procurementApi.post(`/rfqs/${id}/select-quotation/${quotationId}`),
      { loading: 'Awarding...', success: 'Awarded — PO created', error: 'Failed to award' }
    );
    load();
  };

  if (!rfq) return <div className="p-8 text-slate-400 text-sm">Loading...</div>;

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-xl font-black text-slate-900">{rfq.rfqNumber} — {rfq.partName}</h1>
          <p className="text-xs text-slate-500">Qty {rfq.quantity} · Due {new Date(rfq.dueDate).toLocaleString()} · Status {rfq.status}</p>
        </div>
        {rfq.status === 'OPEN' && (
          <button onClick={() => setShowInvite(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-xs font-black rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-200">
            <UserPlus size={14} /> Invite More Suppliers
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-1.5">
        {rfq.invitedSuppliers.map(s => (
          <span key={s.supplierId}
            className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${
              s.hasQuoted ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-50 text-slate-500 border-slate-200'
            }`}>
            {s.supplierName} {s.hasQuoted ? '· Quoted' : '· Pending'}
          </span>
        ))}
      </div>

      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-[10px] font-black uppercase text-slate-400 tracking-widest">
            <tr>
              <th className="text-left px-6 py-3">Supplier</th>
              <th className="text-left px-6 py-3">Unit Price</th>
              <th className="text-left px-6 py-3">Total</th>
              <th className="text-left px-6 py-3">Lead Time</th>
              <th className="text-left px-6 py-3">Status</th>
              <th className="text-right px-6 py-3">Action</th>
            </tr>
          </thead>
          <tbody>
            {quotes.map((q, i) => (
              <tr key={q.id} className={`border-t border-slate-100 ${i === 0 ? 'bg-emerald-50/40' : ''}`}>
                <td className="px-6 py-3 font-bold">{q.supplierName} {i === 0 && <span className="text-[9px] text-emerald-600 font-black ml-1">LOWEST</span>}</td>
                <td className="px-6 py-3">₹{q.unitPrice}</td>
                <td className="px-6 py-3">₹{q.totalPrice}</td>
                <td className="px-6 py-3">{q.leadTimeDays} days</td>
                <td className="px-6 py-3">
                  <span className={`text-[10px] font-black px-2 py-1 rounded-lg ${
                    q.status === 'SELECTED' ? 'bg-emerald-50 text-emerald-700'
                    : q.status === 'REJECTED' ? 'bg-red-50 text-red-500' : 'bg-slate-100 text-slate-600'}`}>
                    {q.status}
                  </span>
                </td>
                <td className="px-6 py-3 text-right">
                  {rfq.status === 'OPEN' && q.status === 'SUBMITTED' && (
                    <button onClick={() => selectWinner(q.id)}
                      className="flex items-center gap-1 ml-auto text-xs font-black text-indigo-600 hover:text-indigo-700">
                      <ShieldCheck size={14} /> Select Winner
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {quotes.length === 0 && (
              <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-300 text-xs font-bold uppercase">No quotations yet</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <InviteMoreSuppliersModal rfq={showInvite ? rfq : null} onClose={() => setShowInvite(false)}
        onSuccess={() => { setShowInvite(false); load(); }} />
    </div>
  );
};

export default RfqDetail;