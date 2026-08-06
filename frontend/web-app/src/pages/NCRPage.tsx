import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import toast from 'react-hot-toast';
import {
  ShieldAlert, Clock, CheckCircle2, FileWarning,
  Loader2, ChevronRight, RotateCcw, Trash2, PackageCheck,
  ClipboardList, PlusCircle, X
} from 'lucide-react';
import { ApiResponse, NonConformanceReport, DispositionRequest } from '../types/inventory';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { CAN_DISPOSITION_NCR, CAN_MANAGE_CAPA, hasAnyRole } from '../utils/permissions';

const dispositionOptions: { value: DispositionRequest['dispositionType']; label: string; icon: React.ReactNode }[] = [
  { value: 'RETURN_TO_VENDOR', label: 'Return to Vendor', icon: <RotateCcw size={16} /> },
  { value: 'SCRAP', label: 'Scrap', icon: <Trash2 size={16} /> },
  { value: 'USE_AS_IS', label: 'Use As-Is (special approval)', icon: <PackageCheck size={16} /> },
];

const NCRPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canDisposition = hasAnyRole(user?.roles, CAN_DISPOSITION_NCR);
  const canManageCapa = hasAnyRole(user?.roles, CAN_MANAGE_CAPA);
  const [ncrs, setNcrs] = useState<NonConformanceReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<NonConformanceReport | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // CAPA Modal state
  const [showCapaModal, setShowCapaModal] = useState(false);
  const [capaTargetNcr, setCapaTargetNcr] = useState<NonConformanceReport | null>(null);
  const [capaSubmitting, setCapaSubmitting] = useState(false);
  const [capaForm, setCapaForm] = useState({
    capaType: 'CORRECTIVE',
    ownerName: '',
    createdBy: '',
    targetCloseDate: '',
    remarks: ''
  });

  const [form, setForm] = useState<DispositionRequest>({
    dispositionType: 'RETURN_TO_VENDOR',
    remarks: '',
    dispositionedBy: '',
  });

  const fetchOpen = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<NonConformanceReport[]>>('/inventory/ncr', { params: { status: 'OPEN' } });
      setNcrs(res.data.data);
    } catch {
      toast.error('Failed to fetch NCR queue.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchOpen(); }, []);

  const handleSelect = (ncr: NonConformanceReport) => {
    setSelected(ncr);
    setForm({ dispositionType: 'RETURN_TO_VENDOR', remarks: '', dispositionedBy: '' });
  };

  const handleOpenCapaModal = (ncr: NonConformanceReport, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setCapaTargetNcr(ncr);
    setCapaForm({ capaType: 'CORRECTIVE', ownerName: '', createdBy: '', targetCloseDate: '', remarks: '' });
    setShowCapaModal(true);
  };

  const handleCreateCapa = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!capaTargetNcr) return;
    if (!capaForm.ownerName.trim() || !capaForm.createdBy.trim()) {
      toast.error('Owner Name and Created By are required');
      return;
    }

    setCapaSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/ncr/${capaTargetNcr.id}/capa`, {
          ...capaForm,
          targetCloseDate: capaForm.targetCloseDate ? `${capaForm.targetCloseDate}T00:00:00` : null,
        }),
        {
          loading: 'Initializing CAPA...',
          success: `CAPA created for NCR ${capaTargetNcr.ncrNumber}`,
          error: (err) => err.response?.data?.message || err.response?.data || 'Failed to open CAPA'
        }
      );
      setShowCapaModal(false);
      navigate('/capa');
    } catch {
    } finally {
      setCapaSubmitting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!form.remarks.trim()) {
      toast.error('Disposition remarks are required for traceability.');
      return;
    }
    if (!form.dispositionedBy.trim()) {
      toast.error('An approver name is required to disposition an NCR.');
      return;
    }

    setSubmitting(true);
    try {
      await toast.promise(
        api.post(`/inventory/ncr/${selected.id}/disposition`, form),
        {
          loading: `Recording ${form.dispositionType} disposition...`,
          success: `NCR ${selected.ncrNumber} dispositioned as ${form.dispositionType}.`,
          error: (err) => err.response?.data?.message || 'Disposition failed.',
        }
      );
      setSelected(null);
      fetchOpen();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-red-600 rounded-2xl text-white">
            <ShieldAlert size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Non-Conformance Reports</h1>
            <p className="text-slate-500 text-sm font-medium">
              Every QA rejection generates an NCR here. Rejected stock stays blocked until dispositioned.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-red-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Open NCR Queue</h2>
            </div>
            <span className="bg-red-100 text-red-700 text-xs font-black px-3 py-1 rounded-full">
              {ncrs.length} OPEN
            </span>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-red-500 mx-auto" size={28} /></div>
          ) : ncrs.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No open NCRs</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {ncrs.map(ncr => (
                <div key={ncr.id} onClick={() => handleSelect(ncr)} role="button" tabIndex={0}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleSelect(ncr); }}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group cursor-pointer ${
                    selected?.id === ncr.id ? 'bg-red-50 border-l-4 border-red-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-red-600 transition-colors">
                        {ncr.ncrNumber}
                      </div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {ncr.partName} · Batch {ncr.batchNumber}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {canManageCapa && (
                        ncr.hasActiveCapa ? (
                          <span
                            title="This NCR already has an active CAPA — see the CAPA page to progress it."
                            className="px-2.5 py-1 bg-slate-100 text-slate-500 rounded-lg text-xs font-bold border border-slate-200 flex items-center gap-1"
                          >
                            <ClipboardList size={12} /> CAPA Already Raised
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={(e) => handleOpenCapaModal(ncr, e)}
                            className="px-2.5 py-1 bg-purple-50 hover:bg-purple-100 text-purple-700 rounded-lg text-xs font-bold border border-purple-200 transition-all flex items-center gap-1"
                          >
                            <ClipboardList size={12} /> Open CAPA
                          </button>
                        )
                      )}
                      <ChevronRight size={16} className="text-slate-300 group-hover:text-red-500 transition-colors" />
                    </div>
                  </div>
                  <div className="mt-2 text-[10px] text-slate-400 font-bold">
                    QTY: {ncr.quantity} units
                  </div>
                  {ncr.reason && (
                    <p className="mt-1 text-xs text-slate-500 italic truncate">{ncr.reason}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <FileWarning size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Disposition</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <ShieldAlert size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select an NCR to disposition</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="flex justify-between items-start">
                  <div>
                    <div className="font-black text-slate-900">{selected.partName}</div>
                    <div className="text-xs text-slate-500 font-medium mt-0.5">
                      Batch: <span className="font-mono font-bold text-red-600">{selected.batchNumber}</span>
                      {' · '} Qty: <span className="font-bold">{selected.quantity}</span>
                    </div>
                  </div>
                  {canManageCapa && (
                    selected.hasActiveCapa ? (
                      <span className="px-3 py-1.5 bg-slate-100 text-slate-500 rounded-xl text-xs font-black flex items-center gap-1.5">
                        <ClipboardList size={14} /> CAPA Already Raised
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleOpenCapaModal(selected)}
                        className="px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-black transition-all flex items-center gap-1.5 shadow-sm"
                      >
                        <ClipboardList size={14} /> Open CAPA
                      </button>
                    )
                  )}
                </div>
                {selected.reason && (
                  <p className="mt-2 text-xs text-slate-600 italic">Rejection reason: {selected.reason}</p>
                )}
                <div className="pt-2">
                  <Link to="/capa" className="text-xs font-bold text-purple-600 hover:text-purple-700">
                    View CAPAs →
                  </Link>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">
                  Disposition *
                </label>
                <div className="space-y-2">
                  {dispositionOptions.map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => setForm({ ...form, dispositionType: opt.value })}
                      className={`w-full py-3 px-4 rounded-xl font-bold text-sm flex items-center gap-2 border-2 transition-all ${
                        form.dispositionType === opt.value
                          ? 'bg-slate-900 text-white border-slate-900'
                          : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                      }`}>
                      {opt.icon} {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                  Remarks * <span className="text-slate-400 font-normal normal-case">(required for traceability)</span>
                </label>
                <textarea required rows={3} value={form.remarks}
                  placeholder="Disposition justification..."
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-red-500/10 focus:border-red-500 outline-none resize-none text-sm"
                  onChange={e => setForm({ ...form, remarks: e.target.value })} />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                  Approver Name * {form.dispositionType === 'USE_AS_IS' && <span className="text-red-500">(special approval signature)</span>}
                </label>
                <input type="text" required value={form.dispositionedBy}
                  placeholder="Full name"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-red-500/10 focus:border-red-500 outline-none text-sm"
                  onChange={e => setForm({ ...form, dispositionedBy: e.target.value })} />
              </div>

              {canDisposition ? (
                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-slate-900 hover:bg-black transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Confirm Disposition
                </button>
              ) : (
                <p className="text-xs text-slate-400 font-bold text-center">Only QA Inspectors, Managers, or Admins can disposition an NCR.</p>
              )}
            </form>
          )}
        </div>
      </div>

      {/* Modal for Opening a CAPA */}
      {showCapaModal && capaTargetNcr && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-5 border border-slate-100">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-purple-100 text-purple-700 rounded-xl">
                  <ClipboardList size={20} />
                </div>
                <div>
                  <h3 className="font-black text-slate-900">Initialize CAPA</h3>
                  <p className="text-xs text-slate-400 font-bold">Against {capaTargetNcr.ncrNumber}</p>
                </div>
              </div>
              <button onClick={() => setShowCapaModal(false)} className="text-slate-400 hover:text-slate-600 p-1">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateCapa} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase block mb-1.5">CAPA Type</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setCapaForm({ ...capaForm, capaType: 'CORRECTIVE' })}
                    className={`py-2 rounded-xl text-xs font-bold border ${capaForm.capaType === 'CORRECTIVE' ? 'bg-purple-600 text-white border-purple-600' : 'bg-slate-50 text-slate-600 border-slate-200'}`}
                  >
                    CORRECTIVE
                  </button>
                  <button
                    type="button"
                    onClick={() => setCapaForm({ ...capaForm, capaType: 'PREVENTIVE' })}
                    className={`py-2 rounded-xl text-xs font-bold border ${capaForm.capaType === 'PREVENTIVE' ? 'bg-purple-600 text-white border-purple-600' : 'bg-slate-50 text-slate-600 border-slate-200'}`}
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
                  value={capaForm.ownerName}
                  onChange={e => setCapaForm({ ...capaForm, ownerName: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Created By *</label>
                <input
                  type="text" required
                  placeholder="Your Name"
                  value={capaForm.createdBy}
                  onChange={e => setCapaForm({ ...capaForm, createdBy: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Target Close Date</label>
                <input
                  type="date"
                  value={capaForm.targetCloseDate}
                  onChange={e => setCapaForm({ ...capaForm, targetCloseDate: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase block mb-1">Initial Remarks</label>
                <textarea
                  rows={2}
                  placeholder="Scope or initial guidance..."
                  value={capaForm.remarks}
                  onChange={e => setCapaForm({ ...capaForm, remarks: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none"
                />
              </div>

              <div className="pt-2 flex gap-2">
                <button
                  type="button" onClick={() => setShowCapaModal(false)}
                  className="flex-1 py-2.5 rounded-xl font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit" disabled={capaSubmitting}
                  className="flex-1 py-2.5 rounded-xl font-black text-white bg-purple-600 hover:bg-purple-700 text-sm flex items-center justify-center gap-2 disabled:opacity-60"
                >
                  {capaSubmitting ? <Loader2 size={16} className="animate-spin" /> : <PlusCircle size={16} />}
                  Start CAPA
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default NCRPage;
