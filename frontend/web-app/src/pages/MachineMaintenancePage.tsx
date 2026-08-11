import React, { useEffect, useState } from 'react';
import productionResourceApi from '../api/productionResourceAxios';
import toast from 'react-hot-toast';
import {
  Wrench, Clock, CheckCircle2, Loader2, ChevronRight, AlertTriangle, PlusCircle, History, Unlock,
} from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Machine, MaintenanceRecord, MaintenanceRequest } from '../types/productionResource';
import { RegisterMachineModal } from '../components/RegisterMachineModal';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../utils/permissions';

const CAN_FORCE_RELEASE = ['PRODUCTION_SUPERVISOR', 'MANAGER', 'ADMIN'];

const MachineMaintenancePage = () => {
  const { user } = useAuth();
  const canForceRelease = hasAnyRole(user?.roles, CAN_FORCE_RELEASE);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Machine | null>(null);
  const [history, setHistory] = useState<MaintenanceRecord[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [dueOnly, setDueOnly] = useState(false);

  const [form, setForm] = useState<MaintenanceRequest>({
    maintenanceType: 'PREVENTIVE', performedBy: '', result: 'COMPLETED', remarks: '',
  });

  const fetchMachines = async () => {
    setLoading(true);
    try {
      const res = dueOnly
        ? await productionResourceApi.get<ApiResponse<Machine[]>>('/resources/machines/due', { params: { withinDays: 30 } })
        : await productionResourceApi.get<ApiResponse<Machine[]>>('/resources/machines');
      setMachines(res.data.data);
    } catch {
      toast.error('Failed to fetch machines.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchMachines(); }, [dueOnly]); // eslint-disable-line react-hooks/exhaustive-deps

  // Escape hatch for a stuck IN_OPERATION status (crash, forgotten completion) —
  // only reachable by supervisors/managers/admins, never by a regular engineer.
  const handleForceRelease = async (m: Machine) => {
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/machines/${m.id}/force-release`),
        { loading: `Releasing ${m.machineCode}...`, success: `${m.machineCode} released`, error: 'Failed to release machine' }
      );
      await fetchMachines();
    } catch {}
  };

  const handleSelect = async (m: Machine) => {
    setSelected(m);
    setForm({ maintenanceType: 'PREVENTIVE', performedBy: '', result: 'COMPLETED', remarks: '' });
    setHistoryLoading(true);
    try {
      const res = await productionResourceApi.get<ApiResponse<MaintenanceRecord[]>>(`/resources/machines/${m.id}/maintenance/history`);
      setHistory(res.data.data);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!form.remarks.trim()) { toast.error('Remarks are mandatory for aerospace traceability.'); return; }

    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/machines/${selected.id}/maintenance`, form),
        {
          loading: 'Recording maintenance...',
          success: `Maintenance recorded for ${selected.machineCode}.`,
          error: (err) => err.response?.data?.message || 'Failed to record maintenance.',
        }
      );
      await fetchMachines();
      const refreshed = machines.find(m => m.id === selected.id);
      if (refreshed) await handleSelect(refreshed);
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const dueLabel = (m: Machine) => {
    if (!m.nextMaintenanceDueAt) return { text: 'No Maintenance Yet', style: 'bg-slate-100 text-slate-500 border-slate-200' };
    if (m.overdue) return { text: `OVERDUE — ${new Date(m.nextMaintenanceDueAt).toLocaleDateString()}`, style: 'bg-red-50 text-red-700 border-red-200' };
    return { text: `Due ${new Date(m.nextMaintenanceDueAt).toLocaleDateString()}`, style: 'bg-amber-50 text-amber-700 border-amber-200' };
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Wrench size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Machine & Maintenance</h1>
            <p className="text-slate-500 text-sm font-medium">
              Machine registry with maintenance due dates and history.
            </p>
          </div>
        </div>
        <button onClick={() => setShowRegister(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
          <PlusCircle size={16} /> Register Machine
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Machine list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-amber-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Machine Register</h2>
            </div>
            <button onClick={() => setDueOnly(!dueOnly)}
              className={`text-[10px] font-black px-3 py-1 rounded-full transition ${dueOnly ? 'bg-amber-500 text-white' : 'bg-slate-100 text-slate-500'}`}>
              {dueOnly ? 'DUE/OVERDUE ONLY' : 'SHOW ALL'}
            </button>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : machines.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">
                {dueOnly ? 'Nothing due for maintenance' : 'No machines registered yet'}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {machines.map(m => {
                const badge = dueLabel(m);
                return (
                  <button key={m.id} onClick={() => handleSelect(m)}
                    className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                      selected?.id === m.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                    }`}>
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">{m.name}</div>
                        <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                          {m.machineCode} · {m.machineType}{m.location ? ` · ${m.location}` : ''}
                        </div>
                      </div>
                      <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                    </div>
                    <span className={`inline-block mt-2 text-[10px] font-black px-2.5 py-1 rounded-full border ${badge.style}`}>
                      {m.overdue && <AlertTriangle size={10} className="inline mr-1 -mt-0.5" />}
                      {badge.text}
                    </span>
                    {m.occupancy === 'IN_OPERATION' && (
                      <span className="inline-block mt-2 ml-1.5 text-[10px] font-black px-2.5 py-1 rounded-full border bg-orange-50 text-orange-700 border-orange-200">
                        IN OPERATION
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* RIGHT — Record maintenance + history */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center gap-2">
              <Wrench size={16} className="text-blue-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Record Maintenance</h2>
            </div>

            {!selected ? (
              <div className="p-16 text-center text-slate-300">
                <Wrench size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">Select a machine to record maintenance</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="p-5 space-y-4">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                  <div className="flex items-center justify-between">
                    <div className="font-black text-slate-900">{selected.name}</div>
                    {selected.occupancy === 'IN_OPERATION' && canForceRelease && (
                      <button type="button" onClick={() => handleForceRelease(selected)}
                        className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-orange-500 text-white hover:bg-orange-600">
                        <Unlock size={10} /> Force Release
                      </button>
                    )}
                  </div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">
                    {selected.machineCode} · every {selected.maintenanceFrequencyDays} days
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Type *</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['PREVENTIVE', 'CORRECTIVE', 'PREDICTIVE'].map(t => (
                      <button key={t} type="button" onClick={() => setForm({ ...form, maintenanceType: t })}
                        className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all ${
                          form.maintenanceType === t ? 'bg-slate-900 text-white border-slate-900' : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                        }`}>
                        {t}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Result *</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['COMPLETED', 'PARTIAL', 'FAILED'].map(r => (
                      <button key={r} type="button" onClick={() => setForm({ ...form, result: r })}
                        className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all ${
                          form.result === r ? 'bg-slate-900 text-white border-slate-900' : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                        }`}>
                        {r}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Performed By</label>
                    <input value={form.performedBy} onChange={e => setForm({ ...form, performedBy: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Downtime (min)</label>
                    <input type="number" min={0} value={form.downtimeMinutes ?? ''}
                      onChange={e => setForm({ ...form, downtimeMinutes: e.target.value ? Number(e.target.value) : undefined })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Cost</label>
                  <input type="number" min={0} step="0.01" value={form.cost ?? ''}
                    onChange={e => setForm({ ...form, cost: e.target.value ? Number(e.target.value) : undefined })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                    Remarks * <span className="text-slate-400 font-normal normal-case">(required for traceability)</span>
                  </label>
                  <textarea required rows={2} value={form.remarks} onChange={e => setForm({ ...form, remarks: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                </div>

                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Record Maintenance
                </button>
              </form>
            )}
          </div>

          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center gap-2">
                <History size={16} className="text-slate-400" />
                <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                  Maintenance History — {selected.machineCode}
                </h2>
              </div>
              {historyLoading ? (
                <div className="p-10 text-center"><Loader2 className="animate-spin text-blue-400 mx-auto" size={20} /></div>
              ) : history.length === 0 ? (
                <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                  No maintenance events recorded yet
                </div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {history.map(r => (
                    <div key={r.id} className="p-4">
                      <div className="flex items-center justify-between mb-1">
                        <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${
                          r.result === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : r.result === 'FAILED' ? 'bg-red-50 text-red-700 border-red-200'
                          : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}>{r.result}</span>
                        <span className="text-[10px] text-slate-400 font-bold">{new Date(r.performedAt).toLocaleString()}</span>
                      </div>
                      <p className="text-xs text-slate-600 font-medium italic">{r.remarks}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-[10px] text-slate-400 font-bold">
                          {r.performedBy || 'Unknown'} · {r.maintenanceType}
                          {r.downtimeMinutes != null && ` · ${r.downtimeMinutes}min downtime`}
                        </span>
                        <span className="text-[10px] text-slate-400 font-bold">Next due {new Date(r.nextDueAt).toLocaleDateString()}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      <RegisterMachineModal isOpen={showRegister} onClose={() => setShowRegister(false)}
        onSuccess={() => { setShowRegister(false); fetchMachines(); }} />
    </div>
  );
};

export default MachineMaintenancePage;
