import React, { useEffect, useState } from 'react';
import qaApi from '../api/qaAxios';
import toast from 'react-hot-toast';
import {
  Gauge, Clock, CheckCircle2, Loader2, ChevronRight, AlertTriangle, PlusCircle, History,
} from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Equipment, CalibrationRecord, CalibrationRequest } from '../types/qa';
import { RegisterEquipmentModal } from '../components/RegisterEquipmentModal';

const CalibrationPage = () => {
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Equipment | null>(null);
  const [history, setHistory] = useState<CalibrationRecord[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [dueOnly, setDueOnly] = useState(false);

  const [form, setForm] = useState<CalibrationRequest>({
    performedBy: '', certificateNumber: '', result: 'PASS', remarks: '',
  });

  const fetchEquipment = async () => {
    setLoading(true);
    try {
      const res = dueOnly
        ? await qaApi.get<ApiResponse<Equipment[]>>('/qa/calibration/equipment/due', { params: { withinDays: 30 } })
        : await qaApi.get<ApiResponse<Equipment[]>>('/qa/calibration/equipment');
      setEquipment(res.data.data);
    } catch {
      toast.error('Failed to fetch equipment.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchEquipment(); }, [dueOnly]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSelect = async (eq: Equipment) => {
    setSelected(eq);
    setForm({ performedBy: '', certificateNumber: '', result: 'PASS', remarks: '' });
    setHistoryLoading(true);
    try {
      const res = await qaApi.get<ApiResponse<CalibrationRecord[]>>(`/qa/calibration/equipment/${eq.id}/history`);
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
        qaApi.post(`/qa/calibration/equipment/${selected.id}/calibrate`, form),
        {
          loading: 'Recording calibration...',
          success: `Calibration recorded for ${selected.assetTag}.`,
          error: (err) => err.response?.data?.message || 'Failed to record calibration.',
        }
      );
      await fetchEquipment();
      const refreshed = equipment.find(e => e.id === selected.id);
      if (refreshed) await handleSelect(refreshed);
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const dueLabel = (eq: Equipment) => {
    if (!eq.nextCalibrationDueAt) return { text: 'Never Calibrated', style: 'bg-slate-100 text-slate-500 border-slate-200' };
    if (eq.overdue) return { text: `OVERDUE — ${new Date(eq.nextCalibrationDueAt).toLocaleDateString()}`, style: 'bg-red-50 text-red-700 border-red-200' };
    return { text: `Due ${new Date(eq.nextCalibrationDueAt).toLocaleDateString()}`, style: 'bg-amber-50 text-amber-700 border-amber-200' };
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Gauge size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Gauge & Tool Calibration</h1>
            <p className="text-slate-500 text-sm font-medium">
              Equipment register with calibration due dates and history.
            </p>
          </div>
        </div>
        <button onClick={() => setShowRegister(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
          <PlusCircle size={16} /> Register Equipment
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Equipment list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-amber-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Equipment Register</h2>
            </div>
            <button onClick={() => setDueOnly(!dueOnly)}
              className={`text-[10px] font-black px-3 py-1 rounded-full transition ${dueOnly ? 'bg-amber-500 text-white' : 'bg-slate-100 text-slate-500'}`}>
              {dueOnly ? 'DUE/OVERDUE ONLY' : 'SHOW ALL'}
            </button>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : equipment.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">
                {dueOnly ? 'Nothing due for calibration' : 'No equipment registered yet'}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {equipment.map(eq => {
                const badge = dueLabel(eq);
                return (
                  <button key={eq.id} onClick={() => handleSelect(eq)}
                    className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                      selected?.id === eq.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                    }`}>
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">{eq.name}</div>
                        <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                          {eq.assetTag} · {eq.equipmentType}{eq.location ? ` · ${eq.location}` : ''}
                        </div>
                      </div>
                      <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                    </div>
                    <span className={`inline-block mt-2 text-[10px] font-black px-2.5 py-1 rounded-full border ${badge.style}`}>
                      {eq.overdue && <AlertTriangle size={10} className="inline mr-1 -mt-0.5" />}
                      {badge.text}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* RIGHT — Record calibration + history */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center gap-2">
              <Gauge size={16} className="text-blue-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Record Calibration</h2>
            </div>

            {!selected ? (
              <div className="p-16 text-center text-slate-300">
                <Gauge size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">Select equipment to record a calibration</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="p-5 space-y-4">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                  <div className="font-black text-slate-900">{selected.name}</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">
                    {selected.assetTag} · every {selected.calibrationFrequencyDays} days
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Result *</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['PASS', 'FAIL', 'ADJUSTED'].map(r => (
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
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Certificate No.</label>
                    <input value={form.certificateNumber} onChange={e => setForm({ ...form, certificateNumber: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
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
                  Record Calibration
                </button>
              </form>
            )}
          </div>

          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center gap-2">
                <History size={16} className="text-slate-400" />
                <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                  Calibration History — {selected.assetTag}
                </h2>
              </div>
              {historyLoading ? (
                <div className="p-10 text-center"><Loader2 className="animate-spin text-blue-400 mx-auto" size={20} /></div>
              ) : history.length === 0 ? (
                <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                  No calibration events recorded yet
                </div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {history.map(r => (
                    <div key={r.id} className="p-4">
                      <div className="flex items-center justify-between mb-1">
                        <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${
                          r.result === 'PASS' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : r.result === 'FAIL' ? 'bg-red-50 text-red-700 border-red-200'
                          : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}>{r.result}</span>
                        <span className="text-[10px] text-slate-400 font-bold">{new Date(r.calibratedAt).toLocaleString()}</span>
                      </div>
                      <p className="text-xs text-slate-600 font-medium italic">{r.remarks}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-[10px] text-slate-400 font-bold">
                          {r.performedBy || 'Unknown'}{r.certificateNumber && ` · ${r.certificateNumber}`}
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

      <RegisterEquipmentModal isOpen={showRegister} onClose={() => setShowRegister(false)}
        onSuccess={() => { setShowRegister(false); fetchEquipment(); }} />
    </div>
  );
};

export default CalibrationPage;
