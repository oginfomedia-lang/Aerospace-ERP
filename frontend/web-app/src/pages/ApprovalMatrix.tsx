import React, { useEffect, useState } from 'react';
import { ShieldCheck, RefreshCw, PlusCircle, Edit3, Trash2, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { ApprovalMatrix as ApprovalMatrixType } from '../types/procurement';
import { ApprovalMatrixModal } from '../components/ApprovalMatrixModal';

const ApprovalMatrixPage = () => {
  const [bands, setBands] = useState<ApprovalMatrixType[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<ApprovalMatrixType | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<ApprovalMatrixType[]>('/approval-matrix');
      setBands(res.data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to load approval bands.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleDelete = async (band: ApprovalMatrixType) => {
    if (!window.confirm(`Delete the ${band.approverRole} band (${band.amountFrom}–${band.amountTo})? Existing in-flight PO approval chains already snapshot their required approver and are unaffected.`)) return;
    try {
      await procurementApi.delete(`/approval-matrix/${band.id}`);
      toast.success('Band deleted.');
      fetchData();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to delete.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Approval Matrix</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">
            Amount-band approval rules for Purchase Orders — configurable per organization, no code changes needed.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={fetchData} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all">
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          <button onClick={() => { setEditing(null); setModalOpen(true); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-sm">
            <PlusCircle size={16} /> New Band
          </button>
        </div>
      </div>

      <div className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
        <AlertTriangle size={16} className="text-amber-600 shrink-0 mt-0.5" />
        <p className="text-xs font-medium text-amber-800">
          A Purchase Order whose amount doesn't fall within any active band is auto-approved with no approval chain at all.
          Make sure your bands cover the full amount range you want to require approval for.
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">Module</th>
              <th className="px-6 py-4">Level</th>
              <th className="px-6 py-4 text-right">Amount From</th>
              <th className="px-6 py-4 text-right">Amount To</th>
              <th className="px-6 py-4">Approver Role</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4">Escalation</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {bands.slice().sort((a, b) => a.moduleCode.localeCompare(b.moduleCode) || a.levelSequence - b.levelSequence).map(band => (
              <tr key={band.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-slate-900">{band.moduleCode}</td>
                <td className="px-6 py-4 text-slate-600">{band.levelSequence}</td>
                <td className="px-6 py-4 text-right font-mono text-slate-700">{band.amountFrom.toLocaleString()}</td>
                <td className="px-6 py-4 text-right font-mono text-slate-700">{band.amountTo.toLocaleString()}</td>
                <td className="px-6 py-4 font-bold text-slate-800">{band.approverRole} {band.approverUsername && (
                  <div className="text-[10px] font-normal text-blue-600 mt-0.5">+ {band.approverUsername}</div>
                )}</td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                    band.isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'
                  }`}>
                    {band.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 text-xs text-slate-600">
                  {band.escalationHours ? `${band.escalationHours}h → ${band.escalationToRole || 'flag only'}` : '—'}
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex justify-end gap-1">
                    <button onClick={() => { setEditing(band); setModalOpen(true); }}
                      className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all">
                      <Edit3 size={15} />
                    </button>
                    <button onClick={() => handleDelete(band)}
                      className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {bands.length === 0 && !loading && (
              <tr>
                <td colSpan={8} className="px-6 py-12 text-center text-slate-500">
                  <ShieldCheck className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No approval bands configured</p>
                  <p className="text-sm mt-1">Without any bands, every Purchase Order is auto-approved with no chain.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      <ApprovalMatrixModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={fetchData}
        band={editing}
      />
    </div>
  );
};

export default ApprovalMatrixPage;
