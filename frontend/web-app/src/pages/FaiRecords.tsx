import React, { useEffect, useState } from 'react';
import qaApi from '../api/qaAxios';
import api from '../api/axios';
import toast from 'react-hot-toast';
import {
  FileCheck2, Clock, CheckCircle2, Loader2, ChevronRight, PlusCircle, Trash2, Printer,
} from 'lucide-react';
import { ApiResponse, Part } from '../types/inventory';
import { FaiRecord, FaiCharacteristic } from '../types/qa';
import { ReceiptModal } from '../components/ReceiptModal';
import { FaiReportDocument } from '../components/FaiReportDocument';

const emptyCharacteristic: FaiCharacteristic = {
  charNo: 1, description: '', requirement: '', measuredResult: '', result: 'PASS', remarks: '',
};

const FaiRecords = () => {
  const [pending, setPending] = useState<FaiRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<FaiRecord | null>(null);
  const [faiCompleted, setFaiCompleted] = useState(false);
  const [history, setHistory] = useState<FaiRecord[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showNewModal, setShowNewModal] = useState(false);
  const [parts, setParts] = useState<Part[]>([]);
  const [newPartId, setNewPartId] = useState<number | ''>('');
  const [printRecord, setPrintRecord] = useState<FaiRecord | null>(null);

  const [batchNumber, setBatchNumber] = useState('');
  const [partRevision, setPartRevision] = useState('');
  const [materialCertRef, setMaterialCertRef] = useState('');
  const [inspectorName, setInspectorName] = useState('');
  const [equipmentAssetTag, setEquipmentAssetTag] = useState('');
  const [equipmentList, setEquipmentList] = useState<{ id: number; assetTag: string; name: string; overdue: boolean }[]>([]);
  const [remarks, setRemarks] = useState('');
  const [overallResult, setOverallResult] = useState<'CONFORMING' | 'NON_CONFORMING'>('CONFORMING');
  const [characteristics, setCharacteristics] = useState<FaiCharacteristic[]>([{ ...emptyCharacteristic }]);

  const fetchPending = async () => {
    setLoading(true);
    try {
      const res = await qaApi.get<ApiResponse<FaiRecord[]>>('/qa/fai/pending');
      setPending(res.data.data);
    } catch {
      toast.error('Failed to fetch FAI queue.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPending();
    qaApi.get<ApiResponse<{ id: number; assetTag: string; name: string; overdue: boolean }[]>>('/qa/calibration/equipment')
      .then(res => setEquipmentList(res.data.data))
      .catch(() => {});
  }, []);

  const openNewModal = () => {
    api.get<ApiResponse<Part[]>>('/inventory').then(res => setParts(res.data.data)).catch(() => {});
    setShowNewModal(true);
  };

  const handleCreateManual = async () => {
    if (!newPartId) { toast.error('Pick a part'); return; }
    const part = parts.find(p => p.id === newPartId);
    try {
      await toast.promise(
        qaApi.post('/qa/fai', { partId: newPartId, partName: part?.name, skuCode: part?.serialNumber }),
        { loading: 'Opening FAI...', success: 'FAI opened', error: (err) => err.response?.data?.message || 'Failed to open FAI' }
      );
      setShowNewModal(false);
      setNewPartId('');
      fetchPending();
    } catch {}
  };

  const fetchHistory = async (partId: number) => {
    setHistoryLoading(true);
    try {
      const res = await qaApi.get<ApiResponse<FaiRecord[]>>(`/qa/fai/part/${partId}/history`);
      setHistory(res.data.data);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleSelect = async (record: FaiRecord) => {
    setSelected(record);
    setFaiCompleted(false);
    setBatchNumber(''); setPartRevision(''); setMaterialCertRef('');
    setInspectorName(''); setEquipmentAssetTag(''); setRemarks(''); setOverallResult('CONFORMING');
    setCharacteristics([{ ...emptyCharacteristic }]);
    await fetchHistory(record.partId);
  };

  const updateCharacteristic = (index: number, patch: Partial<FaiCharacteristic>) => {
    setCharacteristics(prev => prev.map((c, i) => i === index ? { ...c, ...patch } : c));
  };

  const addCharacteristic = () => {
    setCharacteristics(prev => [...prev, { ...emptyCharacteristic, charNo: prev.length + 1 }]);
  };

  const removeCharacteristic = (index: number) => {
    setCharacteristics(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!remarks.trim()) { toast.error('Remarks are mandatory for aerospace traceability.'); return; }
    if (characteristics.length === 0) { toast.error('Add at least one characteristic.'); return; }

    setSubmitting(true);
    try {
      await toast.promise(
        qaApi.post(`/qa/fai/${selected.id}/perform`, {
          batchNumber, partRevision, materialCertRef, inspectorName, equipmentAssetTag, remarks, overallResult, characteristics,
        }),
        {
          loading: 'Completing FAI...',
          success: `FAI ${selected.faiNumber} completed.`,
          error: (err) => err.response?.data?.message || 'Failed to complete FAI.',
        }
      );
      // Mark as completed — keep `selected` alive so the history panel stays visible
      setFaiCompleted(true);
      // Refresh both the pending queue (removes this item) and the history (adds the new COMPLETE record)
      fetchPending();
      await fetchHistory(selected.partId);
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-indigo-600 rounded-2xl text-white">
            <FileCheck2 size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">First Article Inspection</h1>
            <p className="text-slate-500 text-sm font-medium">
              AS9102-lite records — auto-queued on a part's first GRN receipt.
            </p>
          </div>
        </div>
        <button onClick={openNewModal}
          className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 text-white text-sm font-black rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-200">
          <PlusCircle size={16} /> New FAI
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock size={16} className="text-amber-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Pending FAI Queue</h2>
            </div>
            <span className="bg-amber-100 text-amber-700 text-xs font-black px-3 py-1 rounded-full">
              {pending.length} PENDING
            </span>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-indigo-500 mx-auto" size={28} /></div>
          ) : pending.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No pending first articles</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {pending.map(record => (
                <button key={record.id} onClick={() => handleSelect(record)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === record.id ? 'bg-indigo-50 border-l-4 border-indigo-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-indigo-600 transition-colors">
                        {record.faiNumber}
                      </div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {record.partName} {record.skuCode ? `· ${record.skuCode}` : ''}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-indigo-500 transition-colors" />
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center gap-2">
              <FileCheck2 size={16} className="text-indigo-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Perform FAI</h2>
            </div>

            {!selected ? (
              <div className="p-16 text-center text-slate-300">
                <FileCheck2 size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">Select a queued FAI to perform</p>
              </div>
            ) : faiCompleted ? (
              <div className="p-10 text-center space-y-3">
                <CheckCircle2 size={48} className="mx-auto text-emerald-500" />
                <p className="font-black text-slate-800 text-lg">{selected.faiNumber} — Completed!</p>
                <p className="text-slate-500 text-sm">FAI record has been saved. View the history below to print the report.</p>
                <button
                  type="button"
                  onClick={() => { setSelected(null); setFaiCompleted(false); }}
                  className="mt-2 px-5 py-2 text-sm font-bold text-indigo-600 border border-indigo-200 rounded-xl hover:bg-indigo-50 transition"
                >
                  ← Back to Queue
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="p-5 space-y-4">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                  <div className="font-black text-slate-900">{selected.faiNumber}</div>
                  <div className="text-xs text-slate-500 font-medium mt-0.5">{selected.partName}</div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Batch Number</label>
                    <input value={batchNumber} onChange={e => setBatchNumber(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Part Revision</label>
                    <input value={partRevision} onChange={e => setPartRevision(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Material Cert Ref</label>
                    <input value={materialCertRef} onChange={e => setMaterialCertRef(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Inspector</label>
                    <input value={inspectorName} onChange={e => setInspectorName(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Calibrated Tool</label>
                    <select value={equipmentAssetTag} onChange={e => setEquipmentAssetTag(e.target.value)}
                      className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                      <option value="">— None —</option>
                      {equipmentList.map(eq => (
                        <option key={eq.id} value={eq.assetTag}>
                          {eq.assetTag} ({eq.name}){eq.overdue ? ' ⚠️ OVERDUE' : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Overall Result *</label>
                  <div className="grid grid-cols-2 gap-2">
                    {(['CONFORMING', 'NON_CONFORMING'] as const).map(r => (
                      <button key={r} type="button" onClick={() => setOverallResult(r)}
                        className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all ${
                          overallResult === r
                            ? r === 'CONFORMING' ? 'bg-emerald-600 text-white border-emerald-600' : 'bg-red-600 text-white border-red-600'
                            : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                        }`}>
                        {r}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Characteristics *</label>
                    <button type="button" onClick={addCharacteristic}
                      className="flex items-center gap-1 text-[10px] font-black text-indigo-600 hover:text-indigo-700">
                      <PlusCircle size={12} /> Add Row
                    </button>
                  </div>
                  <div className="space-y-2">
                    {characteristics.map((c, i) => (
                      <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-100 space-y-2">
                        <div className="flex items-center gap-2">
                          <input type="number" value={c.charNo ?? ''} onChange={e => updateCharacteristic(i, { charNo: Number(e.target.value) })}
                            placeholder="#" className="w-14 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                          <input value={c.description ?? ''} onChange={e => updateCharacteristic(i, { description: e.target.value })}
                            placeholder="Description" className="flex-1 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                          <button type="button" onClick={() => removeCharacteristic(i)} className="text-red-400 hover:text-red-600">
                            <Trash2 size={14} />
                          </button>
                        </div>
                        <div className="grid grid-cols-3 gap-2">
                          <input value={c.requirement ?? ''} onChange={e => updateCharacteristic(i, { requirement: e.target.value })}
                            placeholder="Requirement" className="px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                          <input value={c.measuredResult ?? ''} onChange={e => updateCharacteristic(i, { measuredResult: e.target.value })}
                            placeholder="Measured" className="px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                          <select value={c.result ?? 'PASS'} onChange={e => updateCharacteristic(i, { result: e.target.value as 'PASS' | 'FAIL' })}
                            className="px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs">
                            <option value="PASS">PASS</option>
                            <option value="FAIL">FAIL</option>
                          </select>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                    Remarks * <span className="text-slate-400 font-normal normal-case">(required for traceability)</span>
                  </label>
                  <textarea required rows={2} value={remarks} onChange={e => setRemarks(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />
                </div>

                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-indigo-600 hover:bg-indigo-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Complete FAI
                </button>
              </form>
            )}
          </div>

          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center gap-2">
                <Clock size={16} className="text-slate-400" />
                <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">FAI History — {selected.partName}</h2>
              </div>
              {historyLoading ? (
                <div className="p-10 text-center"><Loader2 className="animate-spin text-indigo-400 mx-auto" size={20} /></div>
              ) : history.length === 0 ? (
                <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">No FAI history yet</div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {history.map(r => (
                    <div key={r.id} className="p-4">
                      <div className="flex items-center justify-between mb-1">
                        <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${
                          r.status === 'COMPLETE'
                            ? r.overallResult === 'CONFORMING' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-red-50 text-red-700 border-red-200'
                            : 'bg-amber-50 text-amber-700 border-amber-200'
                        }`}>
                          {r.status === 'COMPLETE' ? r.overallResult : r.status}
                        </span>
                        {r.status === 'COMPLETE' && (
                          <button onClick={() => setPrintRecord(r)}
                            className="flex items-center gap-1 text-[10px] font-black text-indigo-600 hover:text-indigo-700">
                            <Printer size={12} /> Print FAI Report
                          </button>
                        )}
                      </div>
                      <p className="text-xs text-slate-600 font-medium">{r.faiNumber}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {showNewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md p-6 space-y-4">
            <h2 className="text-lg font-black text-slate-900">Open New FAI</h2>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase block mb-1">Part</label>
              <select value={newPartId} onChange={e => setNewPartId(e.target.value ? Number(e.target.value) : '')}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="">— Select part —</option>
                {parts.map(p => <option key={p.id} value={p.id}>{p.serialNumber} — {p.name}</option>)}
              </select>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button onClick={() => setShowNewModal(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
              <button onClick={handleCreateManual} className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl">Open FAI</button>
            </div>
          </div>
        </div>
      )}

      <ReceiptModal isOpen={!!printRecord} title="First Article Inspection Report"
        documentNumber={printRecord?.faiNumber || ''} plain onClose={() => setPrintRecord(null)}>
        {printRecord && <FaiReportDocument record={printRecord} />}
      </ReceiptModal>
    </div>
  );
};

export default FaiRecords;
