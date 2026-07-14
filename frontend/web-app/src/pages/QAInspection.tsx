import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import qaApi from '../api/qaAxios';
import {
  ShieldCheck, Clock, CheckCircle2, XCircle,
  Loader2, ChevronRight, AlertTriangle, FileText, User
} from 'lucide-react';
import { Part, StockBalance, ApiResponse, InspectionRequest, InspectionReport } from '../types/inventory';
import toast from 'react-hot-toast';

// This page talks to TWO services:
// api      → inventory-service (localhost:8081) — fetch PENDING_QA batches
// qaApi    → qa-service        (localhost:8082) — submit decisions, fetch history

const statusStyle: Record<string, string> = {
  APPROVED:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  PENDING_QA: 'bg-amber-50 text-amber-700 border-amber-200',
  REJECTED:   'bg-red-50 text-red-700 border-red-200',
};

// One PENDING_QA batch for a part. A part's top-level status/batchNumber only ever
// summarizes its APPROVED stock, so the queue must be built from `part.balances` —
// a part can have several pending batches (or a pending one alongside approved
// stock from an earlier GRN) at the same time.
interface PendingBatch {
  part: Part;
  balance: StockBalance;
}

const QAInspection = () => {
  const [pendingBatches, setPendingBatches] = useState<PendingBatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<PendingBatch | null>(null);
  const [history, setHistory] = useState<InspectionReport[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<InspectionRequest>({
    partId: 0,
    batchNumber: '',
    approvedQty: 0,
    rejectedQty: 0,
    totalQuantity: 0,
    criteria: '',
    remarks: '',
    certificateNumber: '',
    inspectorName: '',
    location: '',
  });

  // Fetch all parts and flatten every PENDING_QA batch across all of them
  const fetchPending = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<Part[]>>('/inventory');
      const pending: PendingBatch[] = [];
      res.data.data.forEach(part => {
        (part.balances || [])
          .filter(b => b.status === 'PENDING_QA')
          .forEach(balance => pending.push({ part, balance }));
      });
      setPendingBatches(pending);
    } catch {
      toast.error('Failed to fetch pending batches.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPending(); }, []);

  // When user clicks a pending batch — populate form and fetch this part's QA history
  const handleSelectBatch = async (entry: PendingBatch) => {
    setSelected(entry);
    setForm({
      partId: entry.part.id,
      batchNumber: entry.balance.batchNumber,
      // Default to fully approved — the inspector edits the split if some of the
      // batch needs to be rejected instead.
      approvedQty: entry.balance.quantity,
      rejectedQty: 0,
      totalQuantity: entry.balance.quantity,
      criteria: '',
      remarks: '',
      certificateNumber: '',
      inspectorName: '',
      location: entry.balance.location || '',
    });

    // Fetch inspection history for this part from qa-service
    setHistoryLoading(true);
    try {
      const res = await qaApi.get<ApiResponse<InspectionReport[]>>(
        `/qa/part/${entry.part.id}/history`
      );
      setHistory(res.data.data);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Submit QA decision → qa-service → Kafka → inventory auto-updates
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.remarks.trim()) {
      toast.error('Remarks are mandatory for aerospace traceability.');
      return;
    }
    if (form.approvedQty + form.rejectedQty !== form.totalQuantity) {
      toast.error(`Approved (${form.approvedQty}) + Rejected (${form.rejectedQty}) must equal the batch total (${form.totalQuantity}).`);
      return;
    }

    const isPartial = form.approvedQty > 0 && form.rejectedQty > 0;
    const isApproved = form.rejectedQty === 0;

    setSubmitting(true);
    try {
      await toast.promise(
        qaApi.post<ApiResponse<InspectionReport>>('/qa/inspect', form),
        {
          loading: 'Processing QA decision for GRN batch...',
          success: (res) =>
            isPartial
              ? `⚠️ Partial: ${form.approvedQty} Accepted / ${form.rejectedQty} Rejected for batch ${res.data.data.batchNumber}.`
              : isApproved
              ? `✅ GRN Approved — OK Qty transferred to store. Stock updated for batch ${res.data.data.batchNumber}.`
              : `❌ GRN Rejected — Material blocked. Batch ${res.data.data.batchNumber} remains in PENDING_QA.`,
          error: (err) => err.response?.data?.message || 'Submission failed.',
        }
      );

      // Refresh pending list and history after decision
      await fetchPending();

      // If the batch we had selected is no longer pending, clear the selection
      const stillPending = pendingBatches.find(
        p => p.part.id === selected?.part.id && p.balance.batchNumber === selected?.balance.batchNumber
      );
      if (!stillPending) {
        setSelected(null);
        setHistory([]);
      } else {
        await handleSelectBatch(stillPending);
      }
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const isExpired = (expiryDate: string | null) =>
    expiryDate ? new Date(expiryDate) < new Date() : false;

  return (
    <div className="space-y-6 animate-in fade-in duration-500">

      {/* Header */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <ShieldCheck size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">GRN Approval — Concern Department</h1>
            <p className="text-slate-500 text-sm font-medium">
              Step 3: Concern department reviews inward GRN and approves material.
              Based on GRN remark, OK Qty will transfer to store and stock will update.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* LEFT — Pending Queue */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-amber-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                GRN Approval Queue
              </h2>
            </div>
            <span className="bg-amber-100 text-amber-700 text-xs font-black px-3 py-1 rounded-full">
              {pendingBatches.length} PENDING APPROVAL
            </span>
          </div>

          {loading ? (
            <div className="p-16 text-center">
              <Loader2 className="animate-spin text-blue-500 mx-auto" size={28} />
            </div>
          ) : pendingBatches.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">All batches inspected</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {pendingBatches.map(entry => {
                const { part, balance } = entry;
                return (
                <button
                  key={`${part.id}-${balance.batchNumber}`}
                  onClick={() => handleSelectBatch(entry)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.part.id === part.id && selected?.balance.batchNumber === balance.batchNumber
                      ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">
                        {part.name}
                      </div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        SKU: {part.serialNumber}
                      </div>
                      <div className="flex items-center gap-2 mt-2 flex-wrap">
                        {balance.batchNumber && (
                          <span className="bg-blue-50 text-blue-600 px-2 py-0.5 rounded text-[10px] font-mono font-bold border border-blue-100">
                            {balance.batchNumber}
                          </span>
                        )}
                        {balance.location && (
                          <span className="text-[10px] text-slate-400 font-bold">
                            📍 {balance.location}
                          </span>
                        )}
                        {isExpired(balance.expiryDate) && (
                          <span className="bg-red-50 text-red-600 px-2 py-0.5 rounded text-[10px] font-black border border-red-100 flex items-center gap-1">
                            <AlertTriangle size={8} /> EXPIRED
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-4">
                      <span className={`text-[10px] font-black px-2 py-1 rounded-full border ${
                        part.criticality === 'HIGH' ? 'bg-red-50 text-red-600 border-red-200' :
                        part.criticality === 'MEDIUM' ? 'bg-amber-50 text-amber-600 border-amber-200' :
                        'bg-emerald-50 text-emerald-600 border-emerald-200'
                      }`}>
                        {part.criticality}
                      </span>
                      <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                    </div>
                  </div>

                  <div className="mt-2 text-[10px] text-slate-400 font-bold">
                    QTY: {balance.quantity} units · {part.partType || 'No type'}
                  </div>
                </button>
                );
              })}
            </div>
          )}
        </div>

        {/* RIGHT — Inspection Form + History */}
        <div className="space-y-6">

          {/* Decision Form */}
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center gap-2">
              <FileText size={16} className="text-blue-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                Department Approval — GRN Review
              </h2>
            </div>

            {!selected ? (
              <div className="p-16 text-center text-slate-300">
                <ShieldCheck size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">
                  Select a GRN batch from the queue to approve
                </p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="p-5 space-y-4">

                {/* Selected batch info */}
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                  <div className="font-black text-slate-900">{selected.part.name}</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">
                    Batch: <span className="font-mono font-bold text-blue-600">{selected.balance.batchNumber}</span>
                    {' · '} Qty: <span className="font-bold">{selected.balance.quantity}</span>
                    {' · '} Location: <span className="font-bold">{selected.balance.location || 'N/A'}</span>
                  </div>
                  {isExpired(selected.balance.expiryDate) && (
                    <div className="mt-2 p-2 bg-red-50 rounded-xl border border-red-100 text-xs text-red-600 font-bold flex items-center gap-2">
                      <AlertTriangle size={12} />
                      Batch expired on {new Date(selected.balance.expiryDate!).toLocaleDateString()} — consider REJECTED
                    </div>
                  )}
                </div>

                {/* Approved / Rejected quantity split */}
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">
                    Approved / Rejected Qty * <span className="text-slate-400 font-normal normal-case">(must sum to {form.totalQuantity})</span>
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <div className="flex items-center gap-2 mb-1 text-emerald-600 font-black text-xs">
                        <CheckCircle2 size={14} /> APPROVED QTY
                      </div>
                      <input
                        type="number" min={0} max={form.totalQuantity} required
                        value={form.approvedQty}
                        onChange={e => {
                          const approvedQty = Math.max(0, Math.min(form.totalQuantity, Number(e.target.value)));
                          setForm({ ...form, approvedQty, rejectedQty: form.totalQuantity - approvedQty });
                        }}
                        className="w-full px-4 py-2.5 bg-emerald-50 border-2 border-emerald-200 rounded-xl focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 outline-none text-sm font-bold"
                      />
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1 text-red-600 font-black text-xs">
                        <XCircle size={14} /> REJECTED QTY
                      </div>
                      <input
                        type="number" min={0} max={form.totalQuantity} required
                        value={form.rejectedQty}
                        onChange={e => {
                          const rejectedQty = Math.max(0, Math.min(form.totalQuantity, Number(e.target.value)));
                          setForm({ ...form, rejectedQty, approvedQty: form.totalQuantity - rejectedQty });
                        }}
                        className="w-full px-4 py-2.5 bg-red-50 border-2 border-red-200 rounded-xl focus:ring-4 focus:ring-red-500/10 focus:border-red-500 outline-none text-sm font-bold"
                      />
                    </div>
                  </div>
                  {form.approvedQty > 0 && form.rejectedQty > 0 && (
                    <p className="mt-2 text-[10px] text-amber-600 font-bold uppercase tracking-wider">
                      ⚠ Partial split — {form.approvedQty} accepted, {form.rejectedQty} rejected (an NCR will be raised for the rejected portion)
                    </p>
                  )}
                </div>

                {/* Criteria */}
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                    Acceptance Criteria <span className="text-slate-400 font-normal normal-case">(spec/standard applied)</span>
                  </label>
                  <textarea
                    rows={2}
                    placeholder="e.g. Visual inspection per drawing rev C, dimensional check ±0.05mm..."
                    value={form.criteria}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none resize-none text-sm"
                    onChange={e => setForm({ ...form, criteria: e.target.value })}
                  />
                </div>

                {/* Remarks */}
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                    QA Remarks * <span className="text-slate-400 font-normal normal-case">(required for traceability)</span>
                  </label>
                  <textarea
                    required
                    rows={3}
                    placeholder="Inspection notes, test results, failure reasons..."
                    value={form.remarks}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none resize-none text-sm"
                    onChange={e => setForm({ ...form, remarks: e.target.value })}
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                      Certificate No.
                    </label>
                    <input
                      type="text"
                      placeholder="CERT-2024-001"
                      value={form.certificateNumber}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none text-sm"
                      onChange={e => setForm({ ...form, certificateNumber: e.target.value })}
                    />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider flex items-center gap-1">
                      <User size={10} /> Inspector
                    </label>
                    <input
                      type="text"
                      placeholder="Inspector name"
                      value={form.inspectorName}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none text-sm"
                      onChange={e => setForm({ ...form, inspectorName: e.target.value })}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className={`w-full py-3 rounded-xl font-black text-white transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg ${
                    form.approvedQty > 0 && form.rejectedQty > 0
                      ? 'bg-amber-500 hover:bg-amber-600 shadow-amber-200'
                      : form.rejectedQty === 0
                      ? 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-200'
                      : 'bg-red-600 hover:bg-red-700 shadow-red-200'
                  } disabled:opacity-60 disabled:cursor-not-allowed`}
                >
                {submitting ? (
                  <Loader2 size={16} className="animate-spin" />
                ) : form.approvedQty > 0 && form.rejectedQty > 0 ? (
                  <><CheckCircle2 size={16} /> Submit Partial Split ({form.approvedQty} / {form.rejectedQty})</>
                ) : form.rejectedQty === 0 ? (
                  <><CheckCircle2 size={16} /> Approve — Transfer OK Qty to Store</>
                ) : (
                  <><XCircle size={16} /> Reject — Block Material</>
                )}
                </button>

              <p className="text-[10px] text-slate-400 text-center font-medium">
                Approval: OK Qty transfers to store and stock updates. Rejection: material remains blocked.
                Partial: each portion moves accordingly, with an NCR raised for the rejected qty.
              </p>
              </form>
            )}
          </div>

          {/* Inspection History for selected part */}
          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center gap-2">
                <Clock size={16} className="text-slate-400" />
                <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                  Inspection History — {selected.balance.batchNumber}
                </h2>
              </div>

              {historyLoading ? (
                <div className="p-10 text-center">
                  <Loader2 className="animate-spin text-blue-400 mx-auto" size={20} />
                </div>
              ) : history.length === 0 ? (
                <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                  No previous inspections for this batch
                </div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {history.map(report => (
                    <div key={report.id} className="p-4">
                      <div className="flex items-center justify-between mb-2">
                        <span className={`px-3 py-1 rounded-full text-[10px] font-black border flex items-center gap-1 ${
                          report.decision === 'APPROVED'
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : report.decision === 'PARTIALLY_APPROVED'
                            ? 'bg-amber-50 text-amber-700 border-amber-200'
                            : 'bg-red-50 text-red-700 border-red-200'
                        }`}>
                          {report.decision === 'REJECTED'
                            ? <XCircle size={10} />
                            : <CheckCircle2 size={10} />}
                          {report.decision === 'PARTIALLY_APPROVED'
                            ? `✓ ${report.approvedQty ?? 0} / ✕ ${report.rejectedQty ?? 0}`
                            : report.decision}
                        </span>
                        <div className="flex items-center gap-2">
                          {report.publishedToInventory ? (
                            <span className="text-[10px] text-emerald-600 font-bold">✓ Synced to Inventory</span>
                          ) : (
                            <span className="text-[10px] text-amber-600 font-bold">⏳ Sync Pending</span>
                          )}
                        </div>
                      </div>
                      {report.criteria && (
                        <p className="text-[10px] text-slate-500 font-bold mb-1">Criteria: {report.criteria}</p>
                      )}
                      <p className="text-xs text-slate-600 font-medium italic">{report.remarks}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-[10px] text-slate-400 font-bold">
                          {report.inspectorName || 'Unknown Inspector'}
                          {report.certificateNumber && ` · ${report.certificateNumber}`}
                        </span>
                        <span className="text-[10px] text-slate-400 font-bold">
                          {new Date(report.inspectedAt).toLocaleString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default QAInspection;
