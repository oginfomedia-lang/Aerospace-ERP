import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import toast from 'react-hot-toast';
import {
  ShieldAlert, Clock, CheckCircle2, FileWarning,
  Loader2, ChevronRight, RotateCcw, Trash2, PackageCheck
} from 'lucide-react';
import { ApiResponse, NonConformanceReport, DispositionRequest } from '../types/inventory';

// Every QA rejection auto-creates an NCR here (PartServiceImpl.processQADecision).
// A REJECTED batch stays blocked in the ledger until its NCR is dispositioned:
// Return to Vendor / Scrap (removes it from the ledger for good) or Use As-Is
// (moves it to APPROVED, under a named special approval).
const dispositionOptions: { value: DispositionRequest['dispositionType']; label: string; icon: React.ReactNode }[] = [
  { value: 'RETURN_TO_VENDOR', label: 'Return to Vendor', icon: <RotateCcw size={16} /> },
  { value: 'SCRAP', label: 'Scrap', icon: <Trash2 size={16} /> },
  { value: 'USE_AS_IS', label: 'Use As-Is (special approval)', icon: <PackageCheck size={16} /> },
];

const NCRPage = () => {
  const [ncrs, setNcrs] = useState<NonConformanceReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<NonConformanceReport | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<DispositionRequest>({
    dispositionType: 'RETURN_TO_VENDOR',
    remarks: '',
    dispositionedBy: '',
  });

  const fetchOpen = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<NonConformanceReport[]>>('/inventory/ncr', { params: { status: 'OPEN' } });
      setNcrs(res.data.data);
    } catch {
      toast.error('Failed to fetch NCR queue.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOpen(); }, []);

  const handleSelect = (ncr: NonConformanceReport) => {
    setSelected(ncr);
    setForm({ dispositionType: 'RETURN_TO_VENDOR', remarks: '', dispositionedBy: '' });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!form.remarks.trim()) {
      toast.error('Disposition remarks are required for traceability.');
      return;
    }
    if (!form.dispositionedBy.trim()) {
      toast.error('An approver name is required to disposition an NCR.');
      return;
    }

    setSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/ncr/${selected.id}/disposition`, form),
        {
          loading: `Recording ${form.dispositionType} disposition...`,
          success: `NCR ${selected.ncrNumber} dispositioned as ${form.dispositionType}.`,
          error: (err) => err.response?.data?.message || 'Disposition failed.',
        }
      );
      setSelected(null);
      fetchOpen();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-red-600 rounded-2xl text-white">
            <ShieldAlert size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Non-Conformance Reports</h1>
            <p className="text-slate-500 text-sm font-medium">
              Every QA rejection generates an NCR here. Rejected stock stays blocked until dispositioned.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-red-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Open NCR Queue</h2>
            </div>
            <span className="bg-red-100 text-red-700 text-xs font-black px-3 py-1 rounded-full">
              {ncrs.length} OPEN
            </span>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-red-500 mx-auto" size={28} /></div>
          ) : ncrs.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No open NCRs</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {ncrs.map(ncr => (
                <button key={ncr.id} onClick={() => handleSelect(ncr)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === ncr.id ? 'bg-red-50 border-l-4 border-red-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-red-600 transition-colors">
                        {ncr.ncrNumber}
                      </div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {ncr.partName} · Batch {ncr.batchNumber}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-red-500 transition-colors" />
                  </div>
                  <div className="mt-2 text-[10px] text-slate-400 font-bold">
                    QTY: {ncr.quantity} units
                  </div>
                  {ncr.reason && (
                    <p className="mt-1 text-xs text-slate-500 italic truncate">{ncr.reason}</p>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <FileWarning size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Disposition</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <ShieldAlert size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select an NCR to disposition</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="font-black text-slate-900">{selected.partName}</div>
                <div className="text-xs text-slate-500 font-medium mt-0.5">
                  Batch: <span className="font-mono font-bold text-red-600">{selected.batchNumber}</span>
                  {' · '} Qty: <span className="font-bold">{selected.quantity}</span>
                </div>
                {selected.reason && (
                  <p className="mt-2 text-xs text-slate-600 italic">Rejection reason: {selected.reason}</p>
                )}
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">
                  Disposition *
                </label>
                <div className="space-y-2">
                  {dispositionOptions.map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => setForm({ ...form, dispositionType: opt.value })}
                      className={`w-full py-3 px-4 rounded-xl font-bold text-sm flex items-center gap-2 border-2 transition-all ${
                        form.dispositionType === opt.value
                          ? 'bg-slate-900 text-white border-slate-900'
                          : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                      }`}>
                      {opt.icon} {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                  Remarks * <span className="text-slate-400 font-normal normal-case">(required for traceability)</span>
                </label>
                <textarea required rows={3} value={form.remarks}
                  placeholder="Disposition justification..."
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-red-500/10 focus:border-red-500 outline-none resize-none text-sm"
                  onChange={e => setForm({ ...form, remarks: e.target.value })} />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                  Approver Name * {form.dispositionType === 'USE_AS_IS' && <span className="text-red-500">(special approval signature)</span>}
                </label>
                <input type="text" required value={form.dispositionedBy}
                  placeholder="Full name"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-red-500/10 focus:border-red-500 outline-none text-sm"
                  onChange={e => setForm({ ...form, dispositionedBy: e.target.value })} />
              </div>

              <button type="submit" disabled={submitting}
                className="w-full py-3 rounded-xl font-black text-white bg-slate-900 hover:bg-black transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                Confirm Disposition
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default NCRPage;
