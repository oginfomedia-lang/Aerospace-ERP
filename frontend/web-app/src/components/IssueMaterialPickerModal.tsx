import React, { useEffect, useMemo, useState } from 'react';
import { X, PackageOpen, Loader2, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axios';
import { Part } from '../types/inventory';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  part: Part | null;
  workOrderId?: number;
  woNumber?: string;
  lineItemId?: number;
  suggestedQuantity?: number;
  issuedTo?: string;
  issuedBy?: string;
  onIssued: (response: Part) => void;
}

// Shows exactly which APPROVED batches a part has and how much is in each, before
// committing an issue — replacing the old "type a total qty, backend picks batches
// silently" flow. Pre-fills an oldest-first suggested split (by batch number, since
// StockBalanceDTO doesn't expose the backend's insertion-order id), fully editable
// down to a manual per-batch allocation.
export const IssueMaterialPickerModal: React.FC<Props> = ({
  isOpen, onClose, part, workOrderId, woNumber, lineItemId, suggestedQuantity, issuedTo, issuedBy, onIssued,
}) => {
  const [desiredQty, setDesiredQty] = useState('');
  const [rows, setRows] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  // No Work Order to explain the destination/purpose of this issue — require the
  // user to say why, instead of it silently defaulting to an anonymous "Production".
  const [purpose, setPurpose] = useState('');
  const requiresPurpose = !workOrderId;

  const approvedBatches = useMemo(() => {
    if (!part) return [];
    return (part.balances || [])
      .filter(b => b.status === 'APPROVED' && (b.quantity || 0) > 0)
      .sort((a, b) => (a.batchNumber || '').localeCompare(b.batchNumber || ''));
  }, [part]);

  useEffect(() => {
    if (isOpen && part) {
      setPurpose('');
      const initialQty = suggestedQuantity ?? 0;
      setDesiredQty(initialQty > 0 ? String(initialQty) : '');
      // Pre-fill oldest-batch-first up to the desired total.
      let remaining = initialQty;
      const initialRows: Record<string, string> = {};
      for (const b of approvedBatches) {
        if (remaining <= 0) break;
        const take = Math.min(remaining, b.quantity || 0);
        if (take > 0) {
          initialRows[b.batchNumber || ''] = String(take);
          remaining -= take;
        }
      }
      setRows(initialRows);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, part]);

  if (!isOpen || !part) return null;

  const totalAvailable = approvedBatches.reduce((sum, b) => sum + (b.quantity || 0), 0);
  const totalAllocated = Object.values(rows).reduce((sum, v) => sum + (Number(v) || 0), 0);
  const desired = Number(desiredQty) || 0;
  const isValid = desired > 0 && totalAllocated === desired
    && approvedBatches.every(b => (Number(rows[b.batchNumber || '']) || 0) <= (b.quantity || 0))
    && (!requiresPurpose || purpose.trim().length > 0);

  const applyFifoToTotal = (total: number) => {
    let remaining = total;
    const next: Record<string, string> = {};
    for (const b of approvedBatches) {
      if (remaining <= 0) break;
      const take = Math.min(remaining, b.quantity || 0);
      if (take > 0) {
        next[b.batchNumber || ''] = String(take);
        remaining -= take;
      }
    }
    setRows(next);
  };

  const handleDesiredQtyChange = (value: string) => {
    setDesiredQty(value);
    const n = Number(value) || 0;
    applyFifoToTotal(n);
  };

  const handleRowChange = (batchNumber: string, value: string) => {
    setRows(prev => ({ ...prev, [batchNumber]: value }));
  };

  const handleSubmit = async () => {
    if (requiresPurpose && !purpose.trim()) {
      toast.error('Issued To / Purpose is required when there is no Work Order.');
      return;
    }
    if (!isValid) {
      toast.error(`Allocated quantities must sum to exactly ${desired} (currently ${totalAllocated}).`);
      return;
    }
    const allocations = Object.entries(rows)
      .map(([batchNumber, qty]) => ({ batchNumber, quantity: Number(qty) || 0 }))
      .filter(a => a.quantity > 0);

    setSubmitting(true);
    try {
      const res = await toast.promise(
        api.post(`/inventory/${part.id}/issue-to-production/allocated`, {
          allocations, workOrderId, woNumber, lineItemId,
          issuedTo: requiresPurpose ? purpose.trim() : issuedTo,
          issuedBy,
        }),
        {
          loading: `Issuing ${desired} units across ${allocations.length} batch(es)...`,
          success: `Issued ${desired} units. Stock updated.`,
          error: (err) => err.response?.data?.message || 'Issue failed.',
        }
      );
      onIssued(res.data.data);
      onClose();
    } catch {
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-200 flex flex-col max-h-[90vh]">
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
          <div>
            <h2 className="text-lg font-black text-slate-900">Issue Material</h2>
            <p className="text-xs font-bold text-slate-400">{part.name} · Available: {totalAvailable} units across {approvedBatches.length} batch(es)</p>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 space-y-4">
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Quantity to Issue</label>
            <input type="number" value={desiredQty} onChange={e => handleDesiredQtyChange(e.target.value)}
              className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          {requiresPurpose && (
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Issued To / Purpose *</label>
              <input type="text" value={purpose} onChange={e => setPurpose(e.target.value)}
                placeholder="e.g. Maintenance, R&D Sample, Shop Floor Consumption"
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
              <p className="text-[10px] text-slate-400 mt-1">
                No Work Order is linked to this issue — record why this material is leaving stock.
              </p>
            </div>
          )}

          {approvedBatches.length === 0 ? (
            <div className="text-center py-10 text-slate-300">
              <PackageOpen size={40} className="mx-auto mb-2 opacity-20" />
              <p className="text-xs font-bold uppercase tracking-widest">No APPROVED stock available</p>
            </div>
          ) : (
            <div className="border border-slate-200 rounded-xl overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 text-xs font-bold uppercase tracking-wider">
                  <tr>
                    <th className="px-4 py-2.5 text-left">Batch</th>
                    <th className="px-4 py-2.5 text-right">Available</th>
                    <th className="px-4 py-2.5">Location</th>
                    <th className="px-4 py-2.5">Expiry</th>
                    <th className="px-4 py-2.5 text-right">Allocate</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {approvedBatches.map(b => {
                    const expired = b.expiryDate && new Date(b.expiryDate) < new Date();
                    const allocated = Number(rows[b.batchNumber || '']) || 0;
                    const overAllocated = allocated > (b.quantity || 0);
                    return (
                      <tr key={b.batchNumber} className={overAllocated ? 'bg-red-50' : ''}>
                        <td className="px-4 py-2.5 font-mono font-bold text-slate-800">
                          {b.batchNumber}
                          {b.poNumber && <div className="text-[10px] font-normal text-slate-400">via {b.poNumber}</div>}
                        </td>
                        <td className="px-4 py-2.5 text-right text-slate-600">{b.quantity}</td>
                        <td className="px-4 py-2.5 text-slate-500">{b.location || '—'}</td>
                        <td className="px-4 py-2.5">
                          {expired ? (
                            <span className="flex items-center gap-1 text-red-600 font-bold text-[11px]">
                              <AlertTriangle size={11} /> Expired
                            </span>
                          ) : b.expiryDate ? new Date(b.expiryDate).toLocaleDateString() : '—'}
                        </td>
                        <td className="px-4 py-2.5 text-right">
                          <input type="number" value={rows[b.batchNumber || ''] || ''}
                            onChange={e => handleRowChange(b.batchNumber || '', e.target.value)}
                            className={`w-24 px-2 py-1.5 border rounded-lg text-sm text-right ${
                              overAllocated ? 'border-red-300 bg-red-50' : 'border-slate-200 bg-white'
                            }`} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
                <tfoot>
                  <tr className="bg-slate-50 border-t border-slate-200 font-bold">
                    <td className="px-4 py-2.5" colSpan={4}>Allocated Total</td>
                    <td className={`px-4 py-2.5 text-right ${totalAllocated === desired ? 'text-emerald-600' : 'text-amber-600'}`}>
                      {totalAllocated} / {desired}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </div>

        <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0">
          <button onClick={onClose} className="flex-1 py-2.5 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
            Cancel
          </button>
          <button onClick={handleSubmit} disabled={!isValid || submitting}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-xl transition-all disabled:opacity-50">
            {submitting ? <Loader2 size={16} className="animate-spin" /> : null} Issue Material
          </button>
        </div>
      </div>
    </div>
  );
};
