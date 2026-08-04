import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { Search, CheckCircle2, XCircle, Loader2, Archive } from 'lucide-react';
import api from '../api/axios';
import qaApi from '../api/qaAxios';
import { ApiResponse, InspectionReport, Part } from '../types/inventory';
import { CertificateOfConformance } from '../types/qa';
import { ReceiptModal } from '../components/ReceiptModal';
import { CertificateOfConformanceDocument } from '../components/CertificateOfConformanceDocument';

type SearchMode = 'part' | 'batch';

const InspectionRecords: React.FC = () => {
  const [mode, setMode] = useState<SearchMode>('part');
  const [parts, setParts] = useState<Part[]>([]);
  const [selectedPartId, setSelectedPartId] = useState<number | ''>('');
  const [batchNumber, setBatchNumber] = useState('');
  const [results, setResults] = useState<InspectionReport[]>([]);
  const [resolvedPart, setResolvedPart] = useState<{ name: string; serialNumber: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [printCoc, setPrintCoc] = useState<CertificateOfConformance | null>(null);

  useEffect(() => {
    api.get<ApiResponse<Part[]>>('/inventory').then(res => setParts(res.data.data)).catch(() => {});
  }, []);

  const search = async () => {
    setLoading(true);
    setSearched(true);
    setResults([]);
    setResolvedPart(null);
    try {
      if (mode === 'part') {
        if (!selectedPartId) { toast.error('Pick a part first.'); return; }
        const res = await qaApi.get<ApiResponse<InspectionReport[]>>(`/qa/part/${selectedPartId}/history`);
        setResults(res.data.data);
        const part = parts.find(p => p.id === selectedPartId);
        if (part) setResolvedPart({ name: part.name, serialNumber: part.serialNumber });
      } else {
        if (!batchNumber.trim()) { toast.error('Enter a batch number first.'); return; }
        const res = await qaApi.get<ApiResponse<InspectionReport[]>>(`/qa/batch/${batchNumber.trim()}/history`);
        setResults(res.data.data);
        if (res.data.data.length > 0) {
          const partRes = await api.get<ApiResponse<Part>>(`/inventory/${res.data.data[0].partId}`);
          setResolvedPart({ name: partRes.data.data.name, serialNumber: partRes.data.data.serialNumber });
        }
      }
    } catch {
      toast.error('Failed to fetch inspection history.');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateCoc = async (report: InspectionReport) => {
    if (!resolvedPart) return;
    try {
      const res = await toast.promise(
        qaApi.post<ApiResponse<CertificateOfConformance>>('/qa/coc', {
          inspectionReportId: report.id,
          partName: resolvedPart.name,
          skuCode: resolvedPart.serialNumber,
          issuedBy: report.inspectorName || 'QA',
        }),
        { loading: 'Preparing certificate...', success: 'Certificate ready', error: 'Failed to generate certificate' }
      );
      setPrintCoc(res.data.data);
    } catch {}
  };

  return (
    <div className="p-8 max-w-4xl">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
          <Archive size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Inspection Records</h1>
          <p className="text-xs text-slate-500 font-medium">
            Full QA history by part or batch — independent of the pending-approval queue
          </p>
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm p-6 space-y-4 mb-6">
        <div className="flex gap-2">
          <button onClick={() => setMode('part')}
            className={`px-4 py-2 rounded-xl text-xs font-black uppercase tracking-wider ${
              mode === 'part' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>
            By Part
          </button>
          <button onClick={() => setMode('batch')}
            className={`px-4 py-2 rounded-xl text-xs font-black uppercase tracking-wider ${
              mode === 'batch' ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600'}`}>
            By Batch Number
          </button>
        </div>

        {mode === 'part' ? (
          <select value={selectedPartId} onChange={e => setSelectedPartId(Number(e.target.value) || '')}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
            <option value="">— Select part —</option>
            {parts.map(p => (
              <option key={p.id} value={p.id}>{p.serialNumber} — {p.name}</option>
            ))}
          </select>
        ) : (
          <input value={batchNumber} onChange={e => setBatchNumber(e.target.value)}
            placeholder="e.g. BATCH-2026-000003"
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        )}

        <button onClick={search} disabled={loading}
          className="w-full py-2.5 bg-indigo-600 text-white font-black rounded-xl disabled:opacity-60 flex items-center justify-center gap-2">
          {loading ? <Loader2 size={16} className="animate-spin" /> : <Search size={16} />}
          Search
        </button>
      </div>

      {searched && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100">
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
              {resolvedPart ? `${resolvedPart.name} (${resolvedPart.serialNumber})` : 'Results'}
            </h2>
          </div>
          {results.length === 0 ? (
            <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
              No inspection records found
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {results.map(report => (
                <div key={report.id} className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className={`px-3 py-1 rounded-full text-[10px] font-black border flex items-center gap-1 ${
                      report.decision === 'APPROVED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : report.decision === 'PARTIALLY_APPROVED' ? 'bg-amber-50 text-amber-700 border-amber-200'
                        : 'bg-red-50 text-red-700 border-red-200'}`}>
                      {report.decision === 'REJECTED' ? <XCircle size={10} /> : <CheckCircle2 size={10} />}
                      {report.decision === 'PARTIALLY_APPROVED'
                        ? `✓ ${report.approvedQty ?? 0} / ✕ ${report.rejectedQty ?? 0}` : report.decision}
                    </span>
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] text-slate-400 font-bold">Batch {report.batchNumber}</span>
                      {(report.decision === 'APPROVED' || report.decision === 'PARTIALLY_APPROVED') && (report.approvedQty ?? 0) > 0 && (
                        <button onClick={() => handleGenerateCoc(report)}
                          className="text-[10px] font-bold text-indigo-600 hover:text-indigo-700">
                          Generate / View CoC
                        </button>
                      )}
                    </div>
                  </div>
                  {report.criteria && <p className="text-[10px] text-slate-500 font-bold mb-1">Criteria: {report.criteria}</p>}
                  <p className="text-xs text-slate-600 font-medium italic">{report.remarks}</p>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-[10px] text-slate-400 font-bold">
                      {report.inspectorName || 'Unknown Inspector'}
                      {report.certificateNumber && ` · ${report.certificateNumber}`}
                    </span>
                    <span className="text-[10px] text-slate-400 font-bold">
                      {new Date(report.inspectedAt).toLocaleString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <ReceiptModal isOpen={!!printCoc} title="Certificate of Conformance"
        documentNumber={printCoc?.cocNumber || ''} plain onClose={() => setPrintCoc(null)}>
        {printCoc && <CertificateOfConformanceDocument coc={printCoc} />}
      </ReceiptModal>
    </div>
  );
};

export default InspectionRecords;
