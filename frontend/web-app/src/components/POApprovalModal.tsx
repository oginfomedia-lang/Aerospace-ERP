import React, { useEffect, useState } from 'react';
import { X, ShieldCheck, CheckCircle2, XCircle, Clock, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { PurchaseOrderResponse, POApprovalHistoryEntry } from '../types/procurement';

interface Props {
  po: PurchaseOrderResponse;
  onClose: () => void;
  onSuccess: () => void;
}

const statusStyle: Record<string, string> = {
  APPROVED: 'text-emerald-600 bg-emerald-50 border-emerald-200',
  REJECTED: 'text-red-600 bg-red-50 border-red-200',
  PENDING: 'text-amber-600 bg-amber-50 border-amber-200',
  WAITING: 'text-slate-400 bg-slate-50 border-slate-200',
};

// PUR-BR-001/PUR-BR-002: visual approval trail (doc section 6.3) + act on the current level
export const POApprovalModal: React.FC<Props> = ({ po, onClose, onSuccess }) => {
  const [history, setHistory] = useState<POApprovalHistoryEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [acting, setActing] = useState<'approve' | 'reject' | null>(null);
  const [remarks, setRemarks] = useState('');

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<POApprovalHistoryEntry[]>(`/purchase-orders/${po.id}/approval-history`);
      setHistory(res.data.sort((a, b) =>
        b.documentVersion - a.documentVersion || a.approvalLevel - b.approvalLevel));
    } catch {
      // toast handled by axios interceptor
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [po.id]);

  // Only the CURRENT version can ever have an actionable row — old versions are frozen audit trail.
  const pendingRow = history.find(h => h.documentVersion === po.documentVersion && h.status === 'PENDING');

  const handleAction = async (action: 'approve' | 'reject') => {
    if (action === 'reject' && !remarks.trim()) {
      toast.error('PUR-BR-002: Rejection remarks are mandatory.');
      return;
    }
    setActing(action);
    try {
      await procurementApi.post(`/purchase-orders/${po.id}/${action}`, { remarks });
      toast.success(action === 'approve' ? 'Level approved.' : 'PO rejected — returned to Draft.');
      onSuccess();
      onClose();
    } catch {
      // toast handled by axios interceptor
    } finally {
      setActing(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[85vh]">
        <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-50 rounded-xl text-indigo-600">
              <ShieldCheck size={20} />
            </div>
            <div>
              <h2 className="text-lg font-black text-slate-900">Approval Chain</h2>
              <p className="text-xs font-bold text-slate-400">PO: {po.poNumber} · v{po.documentVersion}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 space-y-3">
          {loading ? (
            <div className="flex justify-center py-10"><Loader2 className="animate-spin text-indigo-500" size={28} /></div>
          ) : history.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-10">No approval chain for this PO (auto-approved or not yet submitted).</p>
          ) : Array.from(new Set(history.map(h => h.documentVersion))).map(version => (
            <div key={version} className="space-y-2">
              <div className="flex items-center gap-2 pt-1 first:pt-0">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">Version {version}</span>
                {version === po.documentVersion && (
                  <span className="px-1.5 py-0.5 text-[9px] font-black uppercase tracking-wider rounded bg-indigo-100 text-indigo-700 border border-indigo-200">
                    Current
                  </span>
                )}
              </div>
              {history.filter(h => h.documentVersion === version).map(h => (
                <div key={`${version}-${h.approvalLevel}`} className={`p-4 rounded-xl border ${statusStyle[h.status] || statusStyle.WAITING}`}>
                  <div className="flex items-center justify-between">
                    <span className="font-black text-sm flex items-center gap-2">
                      Level {h.approvalLevel} — {h.approverRole}
                      {h.escalated && (
                        <span className="px-1.5 py-0.5 text-[10px] font-black uppercase tracking-wider rounded bg-orange-100 text-orange-700 border border-orange-200">
                          ⚠ Escalated
                        </span>
                      )}
                    </span>
                    <span className="flex items-center gap-1 text-[10px] font-black uppercase tracking-wider">
                      {h.status === 'APPROVED' && <CheckCircle2 size={12} />}
                      {h.status === 'REJECTED' && <XCircle size={12} />}
                      {h.status === 'PENDING' && <Clock size={12} />}
                      {h.status}
                    </span>
                  </div>
                  {h.approverUsername && (
                    <p className="text-xs mt-1 font-medium opacity-80">
                      {h.status === 'REJECTED' ? 'Rejected' : 'Approved'} by {h.approverUsername}
                      {h.approvedAt && ` on ${new Date(h.approvedAt).toLocaleString()}`}
                    </p>
                  )}
                  {h.remarks && <p className="text-xs mt-1 italic opacity-80">"{h.remarks}"</p>}
                </div>
              ))}
            </div>
          ))}
        </div>

        {pendingRow && (
          <div className="p-6 border-t border-slate-100 shrink-0 bg-slate-50 space-y-3">
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest">
              Remarks {acting === 'reject' && '(required)'}
            </label>
            <textarea value={remarks} onChange={e => setRemarks(e.target.value)} rows={2}
              placeholder="Reason / notes for this decision..."
              className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none" />
            <div className="flex justify-end gap-3">
              <button onClick={() => handleAction('reject')} disabled={acting !== null}
                className="px-4 py-2 bg-red-50 hover:bg-red-100 text-red-600 text-sm font-black rounded-xl transition-all disabled:opacity-50 flex items-center gap-2">
                {acting === 'reject' ? <Loader2 size={16} className="animate-spin" /> : <XCircle size={16} />} Reject
              </button>
              <button onClick={() => handleAction('approve')} disabled={acting !== null}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-black rounded-xl transition-all shadow-lg shadow-emerald-200 disabled:opacity-50 flex items-center gap-2">
                {acting === 'approve' ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />} Approve Level {pendingRow.approvalLevel}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
