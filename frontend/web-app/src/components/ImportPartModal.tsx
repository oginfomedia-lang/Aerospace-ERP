import React, { useState } from 'react';
import * as XLSX from 'xlsx';
import { X, Upload, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import api from '../api/axios';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface ParsedRow {
  name: string;
  skuCode: string;
  partType?: string;
  uom?: string;
  reorderLevel?: number;
  criticality?: string;
}

interface ImportRowError {
  rowNumber: number;
  skuCode: string;
  error: string;
}

interface ImportResult {
  successCount: number;
  failureCount: number;
  errors: ImportRowError[];
}

export const ImportPartsModal = ({ isOpen, onClose, onSuccess }: Props) => {
  const [rows, setRows] = useState<ParsedRow[]>([]);
  const [fileName, setFileName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);

  if (!isOpen) return null;

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    setResult(null);

    const reader = new FileReader();
    reader.onload = (evt) => {
      const data = evt.target?.result;
      const workbook = XLSX.read(data, { type: 'binary' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const json = XLSX.utils.sheet_to_json<any>(sheet);

      const parsed: ParsedRow[] = json.map(r => ({
        name: r['Name'] || r['name'] || '',
        skuCode: r['SKU Code'] || r['skuCode'] || r['SKU'] || '',
        partType: r['Part Type'] || r['partType'] || undefined,
        uom: r['UOM'] || r['uom'] || undefined,
        reorderLevel: r['Reorder Level'] ? Number(r['Reorder Level']) : undefined,
        criticality: r['Criticality'] || r['criticality'] || undefined,
      }));
      setRows(parsed);
    };
    reader.readAsBinaryString(file);
  };

  const handleImport = async () => {
    if (rows.length === 0) {
      toast.error('No rows to import — choose a file first.');
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.post('/inventory/import', rows);
      setResult(res.data.data);
      if (res.data.data.successCount > 0) {
        toast.success(`${res.data.data.successCount} part(s) imported.`);
        onSuccess();
      }
      if (res.data.data.failureCount > 0) {
        toast.error(`${res.data.data.failureCount} row(s) failed — see details below.`);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Import failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-200 flex flex-col max-h-[85vh]">
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
          <div>
            <h2 className="text-lg font-black text-slate-900">Import Parts (Excel / CSV)</h2>
            <p className="text-xs font-bold text-slate-400">Columns: Name, SKU Code, Part Type, UOM, Reorder Level, Criticality</p>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 space-y-4">
          <input type="file" accept=".xlsx,.xls,.csv" onChange={handleFile}
            className="w-full text-sm file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:bg-indigo-50 file:text-indigo-700 file:font-bold" />

          {rows.length > 0 && (
            <div className="border border-slate-200 rounded-xl overflow-hidden">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-400 uppercase font-black">
                  <tr>
                    <th className="px-3 py-2">Name</th>
                    <th className="px-3 py-2">SKU</th>
                    <th className="px-3 py-2">Type</th>
                    <th className="px-3 py-2">UOM</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map((r, i) => (
                    <tr key={i}>
                      <td className="px-3 py-1.5 font-bold text-slate-700">{r.name || '—'}</td>
                      <td className="px-3 py-1.5 font-mono text-slate-600">{r.skuCode || '—'}</td>
                      <td className="px-3 py-1.5 text-slate-500">{r.partType || '—'}</td>
                      <td className="px-3 py-1.5 text-slate-500">{r.uom || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="px-3 py-2 text-[10px] text-slate-400 bg-slate-50">{rows.length} row(s) parsed from {fileName}</p>
            </div>
          )}

          {result && (
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm font-bold">
                <CheckCircle2 size={16} className="text-emerald-500" /> {result.successCount} imported
                {result.failureCount > 0 && <span className="text-red-600">· {result.failureCount} failed</span>}
              </div>
              {result.errors.length > 0 && (
                <div className="space-y-1">
                  {result.errors.map((e, i) => (
                    <div key={i} className="flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-1.5">
                      <AlertTriangle size={12} /> Row {e.rowNumber} ({e.skuCode || 'no SKU'}): {e.error}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0">
          <button onClick={onClose} className="flex-1 py-2.5 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
            Close
          </button>
          <button onClick={handleImport} disabled={submitting || rows.length === 0}
            className="flex-[2] flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl transition-all disabled:opacity-50">
            {submitting ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />}
            Import {rows.length > 0 ? `${rows.length} Row(s)` : ''}
          </button>
        </div>
      </div>
    </div>
  );
};