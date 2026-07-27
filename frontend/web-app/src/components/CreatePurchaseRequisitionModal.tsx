import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import api from '../api/axios';
import { Part } from '../types/inventory';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreatePurchaseRequisitionModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [parts, setParts] = useState<Part[]>([]);
  const [partId, setPartId] = useState<number | ''>('');
  const [quantityNeeded, setQuantityNeeded] = useState(1);
  const [requiredByDate, setRequiredByDate] = useState('');
  const [remarks, setRemarks] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      api.get('/inventory').then(res => setParts(res.data.data)).catch(() => {});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const submit = async () => {
    if (!partId) { toast.error('Pick a part'); return; }
    const selected = parts.find(p => p.id === partId);
    setSubmitting(true);
    try {
      await toast.promise(
        procurementApi.post('/purchase-requisitions', {
          partId,
          partName: selected?.name,
          skuCode: selected?.serialNumber,
          quantityNeeded,
          requiredByDate: requiredByDate ? new Date(requiredByDate).toISOString() : undefined,
          remarks,
        }),
        { loading: 'Creating requisition...', success: 'Purchase Requisition created', error: 'Failed to create requisition' }
      );
      onSuccess();
      setPartId('');
      setQuantityNeeded(1);
      setRequiredByDate('');
      setRemarks('');
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">New Purchase Requisition</h2>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Part</label>
          <select value={partId} onChange={e => setPartId(e.target.value ? Number(e.target.value) : '')}
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm">
            <option value="">— Select part —</option>
            {parts.map(p => (
              <option key={p.id} value={p.id}>{p.serialNumber} — {p.name}</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Quantity Needed</label>
            <input type="number" min={1} value={quantityNeeded} onChange={e => setQuantityNeeded(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Required By (optional)</label>
            <input type="datetime-local" value={requiredByDate} onChange={e => setRequiredByDate(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Remarks</label>
          <textarea value={remarks} onChange={e => setRemarks(e.target.value)} rows={2}
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
          <button onClick={submit} disabled={submitting}
            className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
            Create
          </button>
        </div>
      </div>
    </div>
  );
};
