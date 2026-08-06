import React, { useEffect, useState } from 'react';
import { FileMinus, RefreshCw, CheckCircle2 } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { DebitNoteResponse } from '../types/procurement';

const statusStyle: Record<string, string> = {
  OPEN: 'bg-amber-100 text-amber-700 animate-pulse',
  SETTLED: 'bg-emerald-100 text-emerald-700',
};

const DebitNotes = () => {
  const [notes, setNotes] = useState<DebitNoteResponse[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<DebitNoteResponse[]>('/debit-notes');
      setNotes(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleMarkSettled = async (note: DebitNoteResponse) => {
    if (!window.confirm(`Mark ${note.debitNoteNumber} as settled? This means the credit has been accounted for against ${note.supplierName}.`)) return;
    try {
      const res = await procurementApi.post<DebitNoteResponse>(`/debit-notes/${note.id}/mark-settled`);
      setNotes(notes.map(n => n.id === note.id ? res.data : n));
      toast.success(`${res.data.debitNoteNumber} marked settled.`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to mark settled.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Debit Notes</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">
            Auto-raised when a rejected batch is returned to the vendor — credit owed against future payments.
          </p>
        </div>
        <button onClick={fetchData} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all">
          <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
        </button>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">Debit Note</th>
              <th className="px-6 py-4">Supplier</th>
              <th className="px-6 py-4">PO Number</th>
              <th className="px-6 py-4">SKU</th>
              <th className="px-6 py-4 text-right">Qty</th>
              <th className="px-6 py-4 text-right">Amount</th>
              <th className="px-6 py-4">NCR</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {notes.map(note => (
              <tr key={note.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4 font-bold text-slate-900 font-mono">{note.debitNoteNumber}</td>
                <td className="px-6 py-4 font-medium text-slate-700">{note.supplierName}</td>
                <td className="px-6 py-4 text-slate-600 font-mono">{note.poNumber}</td>
                <td className="px-6 py-4 font-mono text-slate-600">{note.skuCode}</td>
                <td className="px-6 py-4 text-right text-slate-600">{note.quantity}</td>
                <td className="px-6 py-4 text-right font-bold text-slate-900">
                  {note.amount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </td>
                <td className="px-6 py-4 text-slate-500 font-mono">{note.ncrNumber}</td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${statusStyle[note.status]}`}>
                    {note.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-right">
                  {note.status === 'OPEN' && (
                    <button onClick={() => handleMarkSettled(note)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm ml-auto">
                      <CheckCircle2 size={14} /> Mark Settled
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {notes.length === 0 && !loading && (
              <tr>
                <td colSpan={9} className="px-6 py-12 text-center text-slate-500">
                  <FileMinus className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No debit notes yet</p>
                  <p className="text-sm mt-1">These are raised automatically when an NCR is returned to the vendor.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      </div>
    </div>
  );
};

export default DebitNotes;