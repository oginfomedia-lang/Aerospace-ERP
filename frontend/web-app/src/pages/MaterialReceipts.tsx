import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import toast from 'react-hot-toast';
import { Truck, PackageCheck, Clock, Loader2, X, CheckCircle2 } from 'lucide-react';
import { ApiResponse, MaterialReceipt, MaterialReceiptItem, FormGrnRequest, Location } from '../types/inventory';
import { ReceiptModal } from '../components/ReceiptModal';

// Step 1→2 of the gate-to-GRN flow: Material Receipts are logged at the gate
// (via Purchase Orders → "Record Material Receipt"); this page is where the GRN
// is actually formed from a receipt, posting stock to PENDING_QA/APPROVED.
const MaterialReceipts = () => {
  const [receipts, setReceipts] = useState<MaterialReceipt[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<MaterialReceipt | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [receiptForView, setReceiptForView] = useState<MaterialReceipt | null>(null);
  const [grnForView, setGrnForView] = useState<{ receipt: MaterialReceipt; item: MaterialReceiptItem } | null>(null);

  const [form, setForm] = useState<FormGrnRequest>({
    location: '',
    expiryDate: '',
    inspectionQty: undefined,
  });
  const [locations, setLocations] = useState<Location[]>([]);

  const fetchReceipts = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<MaterialReceipt[]>>('/inventory/material-receipts');
      setReceipts(res.data.data);
    } catch {
      toast.error('Failed to fetch material receipts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReceipts();
    api.get<Location[]>('/inventory/locations', { params: { activeOnly: true } })
      .then(res => setLocations(res.data)).catch(() => {});
  }, []);

  const handleOpenFormGrn = (receipt: MaterialReceipt) => {
    setSelected(receipt);
    setForm({ location: '', expiryDate: '', inspectionQty: undefined });
  };

  const handleFormGrn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      const payload = {
        location: form.location,
        expiryDate: form.expiryDate ? `${form.expiryDate}T00:00:00` : undefined,
        inspectionQty: form.inspectionQty || undefined,
      };
      const res = await api.post<ApiResponse<MaterialReceipt>>(
        `/inventory/material-receipts/${selected.id}/form-grn`, payload
      );
      toast.success(`GRN formed from ${res.data.data.mrNumber}. Check the QA queue if inspection is required.`);
      setSelected(null);
      fetchReceipts();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to form GRN.');
    } finally {
      setSubmitting(false);
    }
  };

  const pending = receipts.filter(r => r.status === 'RECEIVED_AT_GATE');
  const processed = receipts.filter(r => r.status === 'GRN_FORMED');

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-cyan-600 rounded-2xl text-white">
            <Truck size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Material Receipts</h1>
            <p className="text-slate-500 text-sm font-medium">
              Goods logged at the gate against a PO. Form the GRN here when ready — stock only
              posts to PENDING_QA/APPROVED once the GRN is formed, not at gate receipt.
            </p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock size={16} className="text-amber-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Pending GRN Formation</h2>
          </div>
          <span className="bg-amber-100 text-amber-700 text-xs font-black px-3 py-1 rounded-full">
            {pending.length} AT GATE
          </span>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-cyan-500 mx-auto" size={28} /></div>
        ) : pending.length === 0 ? (
          <div className="p-12 text-center text-slate-300">
            <PackageCheck size={40} className="mx-auto mb-2 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">Nothing waiting at the gate</p>
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
              <tr>
                <th className="px-6 py-3">MR Number</th>
                <th className="px-6 py-3">PO Number</th>
                <th className="px-6 py-3">Supplier</th>
                <th className="px-6 py-3">Challan</th>
                <th className="px-6 py-3">Received By</th>
                <th className="px-6 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {pending.map(r => (
                <tr key={r.id} className="hover:bg-slate-50/50">
                  <td className="px-6 py-4 font-bold font-mono text-slate-900">
                    <button onClick={() => setReceiptForView(r)} title="View / print / download this receipt"
                      className="hover:text-cyan-600 hover:underline transition-colors">
                      {r.mrNumber}
                    </button>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{r.poNumber}</td>
                  <td className="px-6 py-4 text-slate-600">{r.supplierName}</td>
                  <td className="px-6 py-4 text-slate-600">{r.challanNumber}</td>
                  <td className="px-6 py-4 text-slate-600">{r.receivedBy}</td>
                  <td className="px-6 py-4 text-right">
                    <button onClick={() => handleOpenFormGrn(r)}
                      className="px-3 py-1.5 bg-cyan-600 hover:bg-cyan-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm">
                      Form GRN →
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center gap-2">
          <PackageCheck size={16} className="text-emerald-500" />
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">GRN Formed</h2>
        </div>
        {processed.length === 0 ? (
          <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
            No GRNs formed yet
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
              <tr>
                <th className="px-6 py-3">MR Number</th>
                <th className="px-6 py-3">PO Number</th>
                <th className="px-6 py-3">GRN(s)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {processed.map(r => (
                <tr key={r.id} className="hover:bg-slate-50/50">
                  <td className="px-6 py-4 font-bold font-mono text-slate-900">
                    <button onClick={() => setReceiptForView(r)} title="View / print / download this receipt"
                      className="hover:text-cyan-600 hover:underline transition-colors">
                      {r.mrNumber}
                    </button>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{r.poNumber}</td>
                  <td className="px-6 py-4 text-slate-600 font-mono text-xs">
                    {r.items.filter(i => i.grnNumber).length > 0 ? (
                      r.items.filter(i => i.grnNumber).map((i, idx) => (
                        <React.Fragment key={i.id}>
                          {idx > 0 && ', '}
                          <button onClick={() => setGrnForView({ receipt: r, item: i })}
                            title="View / print / download this GRN" className="hover:text-cyan-600 hover:underline transition-colors">
                            {i.grnNumber}
                          </button>
                        </React.Fragment>
                      ))
                    ) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden">
            <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-black text-slate-900">Form GRN</h2>
                <p className="text-xs font-bold text-slate-400">From receipt: {selected.mrNumber}</p>
              </div>
              <button onClick={() => setSelected(null)} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-xl">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleFormGrn} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Storage Location</label>
                <select required value={form.location}
                  onChange={e => setForm({ ...form, location: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none">
                  <option value="" disabled>Select a location...</option>
                  {locations.map(l => (
                    <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Batch Expiry Date (if tracked)</label>
                <input type="date" value={form.expiryDate}
                  onChange={e => setForm({ ...form, expiryDate: e.target.value })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Inspection Qty (optional)</label>
                <input type="number" min={0} value={form.inspectionQty ?? ''}
                  onChange={e => setForm({ ...form, inspectionQty: e.target.value ? Number(e.target.value) : undefined })}
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none" />
              </div>

              <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
                <h3 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-2">Items</h3>
                <div className="space-y-1.5">
                  {selected.items.map(item => (
                    <div key={item.id} className="flex items-center justify-between text-xs">
                      <span className="font-bold text-slate-700">{item.skuCode}</span>
                      <span className="font-mono text-slate-500">{item.quantity} {item.uom}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button type="button" onClick={() => setSelected(null)} disabled={submitting}
                  className="px-5 py-2.5 text-sm font-bold text-slate-500 hover:text-slate-700">
                  Cancel
                </button>
                <button type="submit" disabled={submitting}
                  className="px-5 py-2.5 bg-cyan-600 hover:bg-cyan-700 text-white text-sm font-black rounded-xl shadow-lg shadow-cyan-200 flex items-center gap-2 disabled:opacity-50">
                  {submitting ? <><Loader2 size={16} className="animate-spin" /> Forming...</> : <><CheckCircle2 size={16} /> Form GRN</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {receiptForView && (
        <ReceiptModal
          isOpen={!!receiptForView}
          onClose={() => setReceiptForView(null)}
          title="Material Receipt"
          documentNumber={receiptForView.mrNumber}
        >
          <div className="grid grid-cols-2 gap-4 mb-6 text-sm">
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">PO Number</p>
              <p className="font-bold text-slate-800">{receiptForView.poNumber || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Status</p>
              <p className="font-bold text-slate-800">{receiptForView.status}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Supplier</p>
              <p className="font-bold text-slate-800">{receiptForView.supplierName || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Challan Number</p>
              <p className="font-bold text-slate-800">{receiptForView.challanNumber || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Transport</p>
              <p className="font-bold text-slate-800">{receiptForView.transportName || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Received By</p>
              <p className="font-bold text-slate-800">{receiptForView.receivedBy || '—'}</p>
            </div>
            <div className="col-span-2">
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Received At</p>
              <p className="font-bold text-slate-800">{new Date(receiptForView.receivedAt).toLocaleString()}</p>
            </div>
          </div>

          <table className="w-full text-left text-sm border-t border-slate-200">
            <thead className="text-slate-400 uppercase text-[10px] tracking-wider">
              <tr>
                <th className="py-2">SKU</th>
                <th className="py-2 text-right">Qty</th>
                <th className="py-2 pl-4">GRN Number</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {receiptForView.items.map(item => (
                <tr key={item.id}>
                  <td className="py-2 font-mono font-bold text-slate-700">{item.skuCode}</td>
                  <td className="py-2 text-right text-slate-600">{item.quantity} {item.uom}</td>
                  <td className="py-2 pl-4 font-mono text-xs text-slate-600">{item.grnNumber || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </ReceiptModal>
      )}

      {grnForView && (
        <ReceiptModal
          isOpen={!!grnForView}
          onClose={() => setGrnForView(null)}
          title="Goods Received Note (GRN)"
          documentNumber={grnForView.item.grnNumber || ''}
        >
          <div className="grid grid-cols-2 gap-4 mb-6 text-sm">
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Material Receipt</p>
              <p className="font-bold text-slate-800">{grnForView.receipt.mrNumber}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">PO Number</p>
              <p className="font-bold text-slate-800">{grnForView.receipt.poNumber || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Supplier</p>
              <p className="font-bold text-slate-800">{grnForView.receipt.supplierName || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Challan Number</p>
              <p className="font-bold text-slate-800">{grnForView.receipt.challanNumber || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Transport</p>
              <p className="font-bold text-slate-800">{grnForView.receipt.transportName || '—'}</p>
            </div>
            <div>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Received At</p>
              <p className="font-bold text-slate-800">{new Date(grnForView.receipt.receivedAt).toLocaleString()}</p>
            </div>
          </div>

          <table className="w-full text-left text-sm border-t border-slate-200">
            <thead className="text-slate-400 uppercase text-[10px] tracking-wider">
              <tr>
                <th className="py-2">SKU</th>
                <th className="py-2 text-right">Quantity Received</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              <tr>
                <td className="py-2 font-mono font-bold text-slate-700">{grnForView.item.skuCode}</td>
                <td className="py-2 text-right text-slate-600">{grnForView.item.quantity} {grnForView.item.uom}</td>
              </tr>
            </tbody>
          </table>
        </ReceiptModal>
      )}
    </div>
  );
};

export default MaterialReceipts;
