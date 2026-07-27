import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Send } from 'lucide-react';
import procurementApi from '../api/procurementAxios';
import { Rfq } from '../types/procurement';

const statusStyle: Record<string, string> = {
  OPEN: 'bg-blue-50 text-blue-600',
  AWARDED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-red-50 text-red-600',
};

const Rfqs: React.FC = () => {
  const [rfqs, setRfqs] = useState<Rfq[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    procurementApi.get<Rfq[]>('/rfqs')
      .then(res => setRfqs(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="p-8">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
          <Send size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Requests for Quotation</h1>
          <p className="text-xs text-slate-500 font-medium">Track sent RFQs and compare quotations</p>
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-slate-400">Loading...</p>
      ) : (
        <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-[10px] font-black uppercase text-slate-400 tracking-widest">
              <tr>
                <th className="text-left px-6 py-3">RFQ Number</th>
                <th className="text-left px-6 py-3">Part</th>
                <th className="text-left px-6 py-3">Qty</th>
                <th className="text-left px-6 py-3">Invited</th>
                <th className="text-left px-6 py-3">Due</th>
                <th className="text-left px-6 py-3">Status</th>
                <th className="text-right px-6 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {rfqs.map(r => (
                <tr key={r.id} className="border-t border-slate-100">
                  <td className="px-6 py-3 font-bold text-slate-800">{r.rfqNumber}</td>
                  <td className="px-6 py-3">{r.partName} <span className="text-slate-400">({r.skuCode})</span></td>
                  <td className="px-6 py-3">{r.quantity}</td>
                  <td className="px-6 py-3 text-xs text-slate-500">
                    {r.invitedSuppliers.filter(s => s.hasQuoted).length} / {r.invitedSuppliers.length} quoted
                  </td>
                  <td className="px-6 py-3 text-xs text-slate-500">{new Date(r.dueDate).toLocaleString()}</td>
                  <td className="px-6 py-3">
                    <span className={`text-[10px] font-black px-2 py-1 rounded-lg ${statusStyle[r.status]}`}>{r.status}</span>
                  </td>
                  <td className="px-6 py-3 text-right">
                    <Link to={`/rfqs/${r.id}`} className="text-xs font-bold text-indigo-600 hover:text-indigo-700">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
              {rfqs.length === 0 && (
                <tr><td colSpan={7} className="px-6 py-10 text-center text-slate-300 text-xs font-bold uppercase">No RFQs yet</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default Rfqs;
