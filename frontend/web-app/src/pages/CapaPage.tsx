import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import toast from 'react-hot-toast';
import {
  ClipboardList, Clock, CheckCircle2, Loader2, ChevronRight, XCircle, PlusCircle, X
} from 'lucide-react';
import { ApiResponse, CapaRecord, CapaTransitionRequest, NonConformanceReport } from '../types/inventory';

const stageEndpoint: Record<string, string> = {
  OPEN: 'root-cause',
  ROOT_CAUSE_LOGGED: 'action-plan',
  ACTION_PLANNED: 'start',
  IN_PROGRESS: 'submit-verification',
  PENDING_VERIFICATION: 'close',
};

const stageActionLabel: Record<string, string> = {
  OPEN: 'Log Root Cause',
  ROOT_CAUSE_LOGGED: 'Plan Corrective Action',
  ACTION_PLANNED: 'Start Implementation',
  IN_PROGRESS: 'Submit for Verification',
  PENDING_VERIFICATION: 'Verify & Close',
};

const CapaPage = () => {
  const [capas, setCapas] = useState<CapaRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<CapaRecord | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [showOpenModal, setShowOpenModal] = useState(false);
  const [ncrs, setNcrs] = useState<NonConformanceReport[]>([]);
  const [newNcrId, setNewNcrId] = useState<number | ''>('');
  const [capaType, setCapaType] = useState<'CORRECTIVE' | 'PREVENTIVE'>('CORRECTIVE');
  const [ownerName, setOwnerName] = useState('');
  const [targetCloseDate, setTargetCloseDate] = useState('');
  const [openRemarks, setOpenRemarks] = useState('');
  const [createdBy, setCreatedBy] = useState('');
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const [rootCause, setRootCause] = useState('');
  const [correctiveAction, setCorrectiveAction] = useState('');
  const [preventiveAction, setPreventiveAction] = useState('');
  const [verificationRemarks, setVerificationRemarks] = useState('');
  const [remarks, setRemarks] = useState('');
  const [actor, setActor] = useState('');

  const fetchAll = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<CapaRecord[]>>('/inventory/capa');
      setCapas(res.data.data);
    } catch {
      toast.error('Failed to fetch CAPA list.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const handleOpenNewModal = async () => {
    try {
      const res = await api.get<ApiResponse<NonConformanceReport[]>>('/inventory/ncr', { params: { status: 'OPEN' } });
      setNcrs(res.data.data);
      if (res.data.data.length > 0) {
        setNewNcrId(res.data.data[0].id);
      }
      setShowOpenModal(true);
    } catch {
      toast.error('Failed to fetch open NCRs.');
    }
  };

  const handleCreateCapa = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNcrId) {
      toast.error('Select an NCR');
      return;
    }
    if (!ownerName.trim() || !createdBy.trim()) {
      toast.error('Owner Name and Created By are required');
      return;
    }

    setCreateSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/ncr/${newNcrId}/capa`, {
          capaType,
          ownerName,
          createdBy,
          targetCloseDate,
          remarks: openRemarks
        }),
        {
          loading: 'Initializing CAPA...',
          success: 'CAPA initialized successfully',
          error: (err) => err.response?.data?.message || 'Failed to open CAPA'
        }
      );
      setShowOpenModal(false);
      setNewNcrId(''); setOwnerName(''); setCreatedBy(''); setTargetCloseDate(''); setOpenRemarks('');
      fetchAll();
    } catch {
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleSelect = (capa: CapaRecord) => {
    setSelected(capa);
    setRootCause(''); setCorrectiveAction(''); setPreventiveAction('');
    setVerificationRemarks(''); setRemarks(''); setActor('');
  };

  const handleAdvance = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!remarks.trim() || !actor.trim()) { toast.error('Remarks and your name are required.'); return; }

    const endpoint = stageEndpoint[selected.status];
    if (!endpoint) return;

    const payload: CapaTransitionRequest = { remarks, actor, rootCause, correctiveAction, preventiveAction, verificationRemarks };

    setSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/capa/${selected.id}/${endpoint}`, payload),
        {
          loading: 'Advancing CAPA...',
          success: `${selected.capaNumber} advanced.`,
          error: (err) => err.response?.data?.message || 'Failed to advance CAPA.',
        }
      );
      setSelected(null);
      fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!selected) return;
    if (!remarks.trim() || !actor.trim()) { toast.error('Remarks and your name are required to reject.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/capa/${selected.id}/reject`, { remarks, actor }),
        { loading: 'Rejecting CAPA...', success: `${selected.capaNumber} rejected.`, error: 'Failed to reject CAPA.' }
      );
      setSelected(null);
      fetchAll();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-purple-600 rounded-2xl text-white">
            <ClipboardList size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">CAPA — Corrective & Preventive Actions</h1>
            <p className="text-slate-500 text-sm font-medium">
              Root cause → action plan → implementation → verification, tracked per NCR.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={handleOpenNewModal}
          className="px-4 py-2.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-black transition-all flex items-center gap-2 shadow-lg active:scale-95 shrink-0"
        >
          <PlusCircle size={16} /> Open New CAPA
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <Clock size={16} className="text-purple-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">All CAPAs</h2>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-purple-500 mx-auto" size={28} /></div>
          ) : capas.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No CAPAs opened yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {capas.map(capa => (
                <button key={capa.id} onClick={() => handleSelect(capa)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === capa.id ? 'bg-purple-50 border-l-4 border-purple-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-purple-600 transition-colors">{capa.capaNumber}</div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        NCR {capa.ncrNumber} · {capa.ncrPartName}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-purple-500 transition-colors" />
                  </div>
                  <span className={`inline-block mt-2 text-[10px] font-black px-2.5 py-1 rounded-full border ${
                    capa.status === 'CLOSED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : capa.status === 'REJECTED' ? 'bg-red-50 text-red-700 border-red-200'
                    : 'bg-amber-50 text-amber-700 border-amber-200'
                  }`}>
                    {capa.status.replace(/_/g, ' ')}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <ClipboardList size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Advance Stage</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <ClipboardList size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select a CAPA to work on</p>
            </div>
          ) : selected.status === 'CLOSED' || selected.status === 'REJECTED' ? (
            <div className="p-5">
              <p className="text-sm text-slate-500 font-bold mb-4">{selected.capaNumber} is {selected.status}.</p>
              <div className="space-y-2">
                {selected.history.map((h, i) => (
                  <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                    <div className="font-bold text-slate-700">{h.fromStatus ?? '—'} → {h.toStatus}</div>
                    <div className="text-slate-500">{h.actor} · {new Date(h.occurredAt).toLocaleString()}</div>
                    <div className="text-slate-400 italic mt-1">{h.remarks}</div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <form onSubmit={handleAdvance} className="p-5 space-y-4">
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="font-black text-slate-900">{selected.capaNumber}</div>
                <div className="text-xs text-slate-500 font-medium mt-0.5">
                  Current stage: <b>{selected.status.replace(/_/g, ' ')}</b>
                </div>
              </div>

              {selected.status === 'OPEN' && (
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Root Cause *</label>
                  <textarea required rows={3} value={rootCause} onChange={e => setRootCause(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                </div>
              )}

              {selected.status === 'ROOT_CAUSE_LOGGED' && (
                <>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Corrective Action *</label>
                    <textarea required rows={2} value={correctiveAction} onChange={e => setCorrectiveAction(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Preventive Action</label>
                    <textarea rows={2} value={preventiveAction} onChange={e => setPreventiveAction(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                  </div>
                </>
              )}

              {selected.status === 'PENDING_VERIFICATION' && (
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Verification Remarks *</label>
                  <textarea required rows={2} value={verificationRemarks} onChange={e => setVerificationRemarks(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Your Name *</label>
                  <input required value={actor} onChange={e => setActor(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Remarks *</label>
                  <input required value={remarks} onChange={e => setRemarks(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>
              </div>

              <div className="flex gap-2">
                <button type="submit" disabled={submitting}
                  className="flex-1 py-3 rounded-xl font-black text-white bg-purple-600 hover:bg-purple-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  {stageActionLabel[selected.status]}
                </button>
                <button type="button" onClick={handleReject} disabled={submitting}
                  className="px-4 py-3 rounded-xl font-black text-red-600 bg-red-50 hover:bg-red-100 transition-all disabled:opacity-60">
                  <XCircle size={16} />
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      {/* Modal for Opening a New CAPA */}
      {showOpenModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-5 border border-slate-100">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-purple-100 text-purple-700 rounded-xl">
                  <ClipboardList size={20} />
                </div>
                <div>
                  <h3 className="font-black text-slate-900">Open New CAPA</h3>
                  <p className="text-xs text-slate-400 font-bold">Select an open NCR to initialize action</p>
                </div>
              </div>
              <button onClick={() => setShowOpenModal(false)} className="text-slate-400 hover:text-slate-600 p-1">
                <X size={20} />
              </button>
            </div>

            {ncrs.length === 0 ? (
              <div className="p-8 text-center text-slate-400">
                <p className="font-bold text-sm">No open NCRs available to link a CAPA against.</p>
                <button
                  type="button" onClick={() => setShowOpenModal(false)}
                  className="mt-4 px-4 py-2 bg-slate-100 text-slate-700 font-bold rounded-xl text-xs"
                >
                  Close
                </button>
              </div>
            ) : (
              <form onSubmit={handleCreateCapa} className="space-y-4">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Select NCR *</label>
                  <select
                    value={newNcrId}
                    onChange={e => setNewNcrId(Number(e.target.value))}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium"
                  >
                    {ncrs.map(ncr => (
                      <option key={ncr.id} value={ncr.id}>
                        {ncr.ncrNumber} — {ncr.partName} (Batch {ncr.batchNumber})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1.5">CAPA Type</label>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setCapaType('CORRECTIVE')}
                      className={`py-2 rounded-xl text-xs font-bold border ${capaType === 'CORRECTIVE' ? 'bg-purple-600 text-white border-purple-600' : 'bg-slate-50 text-slate-600 border-slate-200'}`}
                    >
                      CORRECTIVE
                    </button>
                    <button
                      type="button"
                      onClick={() => setCapaType('PREVENTIVE')}
                      className={`py-2 rounded-xl text-xs font-bold border ${capaType === 'PREVENTIVE' ? 'bg-purple-600 text-white border-purple-600' : 'bg-slate-50 text-slate-600 border-slate-200'}`}
                    >
                      PREVENTIVE
                    </button>
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Owner Name *</label>
                  <input
                    type="text" required
                    placeholder="Responsible Quality Engineer"
                    value={ownerName}
                    onChange={e => setOwnerName(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Created By *</label>
                  <input
                    type="text" required
                    placeholder="Your Name"
                    value={createdBy}
                    onChange={e => setCreatedBy(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Target Close Date</label>
                  <input
                    type="date"
                    value={targetCloseDate}
                    onChange={e => setTargetCloseDate(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Initial Remarks</label>
                  <textarea
                    rows={2}
                    placeholder="Scope or initial guidance..."
                    value={openRemarks}
                    onChange={e => setOpenRemarks(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none"
                  />
                </div>

                <div className="pt-2 flex gap-2">
                  <button
                    type="button" onClick={() => setShowOpenModal(false)}
                    className="flex-1 py-2.5 rounded-xl font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 text-sm"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit" disabled={createSubmitting}
                    className="flex-1 py-2.5 rounded-xl font-black text-white bg-purple-600 hover:bg-purple-700 text-sm flex items-center justify-center gap-2 disabled:opacity-60"
                  >
                    {createSubmitting ? <Loader2 size={16} className="animate-spin" /> : <PlusCircle size={16} />}
                    Start CAPA
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default CapaPage;
