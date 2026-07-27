import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { Rfq } from '../types/procurement';

interface Supplier { id: number; name: string; certificationStatus: string; active: boolean; }

interface Props {
  rfq: Rfq | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const InviteMoreSuppliersModal: React.FC<Props> = ({ rfq, onClose, onSuccess }) => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (rfq) {
      setSelected([]);
      procurementApi.get<Supplier[]>('/suppliers').then(res => setSuppliers(res.data)).catch(() => {});
    }
  }, [rfq]);

  if (!rfq) return null;

  const alreadyInvitedIds = new Set(rfq.invitedSuppliers.map(s => s.supplierId));
  const eligibleSuppliers = suppliers.filter(s =>
    s.active && s.certificationStatus === 'CERTIFIED' && !alreadyInvitedIds.has(s.id));

  const toggle = (id: number) =>
    setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);

  const submit = async () => {
    if (selected.length === 0) { toast.error('Pick at least one supplier'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        procurementApi.post(`/rfqs/${rfq.id}/invite-suppliers`, { supplierIds: selected }),
        { loading: 'Inviting...', success: 'Invited — they can now submit a quote', error: (err) => err.response?.data?.message || 'Failed to invite' }
      );
      onSuccess();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Invite More Suppliers — {rfq.rfqNumber}</h2>
        <p className="text-xs text-slate-500">Already-invited suppliers aren't shown again below.</p>

        <div className="max-h-56 overflow-y-auto space-y-1 border border-slate-200 rounded-xl p-2">
          {eligibleSuppliers.map(s => (
            <label key={s.id} className="flex items-center gap-2 text-sm p-1.5 rounded-lg hover:bg-slate-50">
              <input type="checkbox" checked={selected.includes(s.id)} onChange={() => toggle(s.id)} />
              {s.name}
            </label>
          ))}
          {eligibleSuppliers.length === 0 && (
            <p className="text-xs text-slate-400 p-2">No other certified suppliers left to invite.</p>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button onClick={onClose} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
          <button onClick={submit} disabled={submitting || eligibleSuppliers.length === 0}
            className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
            Invite
          </button>
        </div>
      </div>
    </div>
  );
};
