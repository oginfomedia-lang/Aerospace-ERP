import React, { useEffect, useState } from 'react';
import { X, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { ApprovalMatrix, ApprovalMatrixRequest } from '../types/procurement';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  band: ApprovalMatrix | null; // null = create
}

export const ApprovalMatrixModal: React.FC<Props> = ({ isOpen, onClose, onSuccess, band }) => {
  const [moduleCode, setModuleCode] = useState('PURCHASE_ORDER');
  const [amountFrom, setAmountFrom] = useState('');
  const [amountTo, setAmountTo] = useState('');
  const [approverRole, setApproverRole] = useState('');
  const [levelSequence, setLevelSequence] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [escalationHours, setEscalationHours] = useState('');
  const [escalationToRole, setEscalationToRole] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (band) {
        setModuleCode(band.moduleCode);
        setAmountFrom(String(band.amountFrom));
        setAmountTo(String(band.amountTo));
        setApproverRole(band.approverRole);
        setLevelSequence(String(band.levelSequence));
        setIsActive(band.isActive);
        setEscalationHours(band.escalationHours != null ? String(band.escalationHours) : '');
        setEscalationToRole(band.escalationToRole ?? '');
      } else {
        setModuleCode('PURCHASE_ORDER'); setAmountFrom(''); setAmountTo('');
        setApproverRole(''); setLevelSequence(''); setIsActive(true);
        setEscalationHours(''); setEscalationToRole('');
      }
    }
  }, [isOpen, band]);

  if (!isOpen) return null;

  const handleSave = async () => {
    if (!moduleCode.trim() || !approverRole.trim() || !amountFrom || !amountTo || !levelSequence) {
      toast.error('All fields are required.');
      return;
    }
    if (Number(amountFrom) >= Number(amountTo)) {
      toast.error('Amount From must be less than Amount To.');
      return;
    }
    setSaving(true);
    try {
      const payload: ApprovalMatrixRequest = {
        moduleCode, amountFrom: Number(amountFrom), amountTo: Number(amountTo),
        approverRole, levelSequence: Number(levelSequence), isActive,
        escalationHours: escalationHours.trim() ? Number(escalationHours) : null,
        escalationToRole: escalationToRole.trim() ? escalationToRole.trim() : null,
      };
      if (band) {
        await procurementApi.put(`/approval-matrix/${band.id}`, payload);
        toast.success('Approval band updated.');
      } else {
        await procurementApi.post('/approval-matrix', payload);
        toast.success('Approval band created.');
      }
      onSuccess();
      onClose();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to save approval band.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-200 flex flex-col">
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center">
          <h2 className="text-lg font-black text-slate-900">{band ? 'Edit' : 'New'} Approval Band</h2>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Module Code *</label>
            <input value={moduleCode} onChange={e => setModuleCode(e.target.value)}
              className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono outline-none focus:ring-2 focus:ring-blue-500" />
            <p className="text-[10px] text-slate-400 mt-1">Only "PURCHASE_ORDER" is currently used by the approval logic.</p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Amount From *</label>
              <input type="number" value={amountFrom} onChange={e => setAmountFrom(e.target.value)}
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Amount To *</label>
              <input type="number" value={amountTo} onChange={e => setAmountTo(e.target.value)}
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Approver Role *</label>
              <input value={approverRole} onChange={e => setApproverRole(e.target.value)}
                placeholder="e.g. MANAGER" className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Level Sequence *</label>
              <input type="number" value={levelSequence} onChange={e => setLevelSequence(e.target.value)}
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input type="checkbox" checked={isActive} onChange={e => setIsActive(e.target.checked)} /> Active
          </label>

          <div className="grid grid-cols-2 gap-4 pt-2 border-t border-slate-100">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Escalation Hours</label>
              <input type="number" value={escalationHours} onChange={e => setEscalationHours(e.target.value)}
                placeholder="e.g. 24" className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
              <p className="text-[10px] text-slate-400 mt-1">Optional. Leave blank for no escalation.</p>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Escalation To Role</label>
              <input value={escalationToRole} onChange={e => setEscalationToRole(e.target.value)}
                placeholder="e.g. MANAGER" className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500" />
              <p className="text-[10px] text-slate-400 mt-1">Optional. Blank = flag only, same approver.</p>
            </div>
          </div>
        </div>

        <div className="flex gap-3 p-6 pt-2 border-t border-slate-100">
          <button onClick={onClose} className="flex-1 py-2.5 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
            Cancel
          </button>
          <button onClick={handleSave} disabled={saving}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-xl transition-all disabled:opacity-60">
            {saving ? <Loader2 size={16} className="animate-spin" /> : null} {band ? 'Save Changes' : 'Create Band'}
          </button>
        </div>
      </div>
    </div>
  );
};
