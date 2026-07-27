import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { PurchaseRequisition } from '../types/procurement';

interface Supplier { id: number; name: string; certificationStatus: string; active: boolean; }

interface Props {
  pr: PurchaseRequisition | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateRfqModal: React.FC<Props> = ({ pr, onClose, onSuccess }) => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [quantity, setQuantity] = useState(1);
  const [dueDate, setDueDate] = useState('');
  const [specification, setSpecification] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (pr) {
      setQuantity(pr.quantityNeeded);
      procurementApi.get<Supplier[]>('/suppliers').then(res =>
        setSuppliers(res.data.filter(s => s.active && s.certificationStatus === 'CERTIFIED')));
    }
  }, [pr]);

  if (!pr) return null;

  const toggle = (id: number) =>
    setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);

  const submit = async () => {
    if (selected.length === 0) { toast.error('Pick at least one supplier'); return; }
    if (!dueDate) { toast.error('Set a quote-by due date'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        procurementApi.post(`/purchase-requisitions/${pr.id}/create-rfq`, {
          supplierIds: selected, quantity, specification,
          dueDate: new Date(dueDate).toISOString(),
        }),
        { loading: 'Creating RFQ...', success: 'RFQ created and suppliers notified', error: 'Failed to create RFQ' }
      );
      onSuccess();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Create RFQ — {pr.prNumber}</h2>
        <p className="text-xs text-slate-500">{pr.partName} ({pr.skuCode})</p>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Invite Suppliers</label>
          <div className="max-h-40 overflow-y-auto space-y-1 border border-slate-200 rounded-xl p-2">
            {suppliers.map(s => (
              <label key={s.id} className="flex items-center gap-2 text-sm p-1.5 rounded-lg hover:bg-slate-50">
                <input type="checkbox" checked={selected.includes(s.id)} onChange={() => toggle(s.id)} />
                {s.name}
              </label>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Quantity</label>
            <input type="number" min={1} value={quantity} onChange={e => setQuantity(Number(e.target.value))}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Quote By</label>
            <input type="datetime-local" value={dueDate} onChange={e => setDueDate(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Specification / Notes</label>
          <textarea value={specification} onChange={e => setSpecification(e.target.value)} rows={2}
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
          <button onClick={submit} disabled={submitting}
            className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
            Send RFQ
          </button>
        </div>
      </div>
    </div>
  );
};