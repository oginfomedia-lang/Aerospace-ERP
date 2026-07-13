import React, { useState } from 'react';
import { PurchaseOrderResponse } from '../types/procurement';
import { X, CheckCircle2, Loader2, Package } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axios';

interface Props {
  po: PurchaseOrderResponse;
  onClose: () => void;
  onSuccess: () => void;
}

// Step 1 of gate-to-GRN: logs goods physically received at the gate against this PO.
// No stock or GRN exists yet — that only happens later, from the Material Receipts
// page, via "Form GRN". This modal deliberately does NOT collect location/inspection
// qty (those apply when the GRN is actually formed) or a GRN number (GRN-FR-001:
// always system-generated, never entered here).
export const MaterialReceiptFromPOModal: React.FC<Props> = ({ po, onClose, onSuccess }) => {
  const [submitting, setSubmitting] = useState(false);
  const [challanNumber, setChallanNumber] = useState('');
  const [transportName, setTransportName] = useState('');
  const [receivedBy, setReceivedBy] = useState('Store Keeper');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      // Sanity-check each SKU against Inventory now, so a bad PO line item is caught
      // here rather than later when someone tries to form the GRN from this receipt.
      const resolved = await Promise.all(
        po.items.map(async (item) => {
          try {
            await api.get(`/inventory/sku/${item.skuCode}`);
            return { ...item, ok: true as const };
          } catch {
            return { ...item, ok: false as const };
          }
        })
      );

      const validItems = resolved.filter(i => i.ok);
      const unresolvedSkus = resolved.filter(i => !i.ok).map(i => i.skuCode);

      if (validItems.length === 0) {
        toast.error(`Could not resolve any of these SKUs in Inventory: ${unresolvedSkus.join(', ')}. Edit the PO and pick real items first.`);
        setSubmitting(false);
        return;
      }

      if (unresolvedSkus.length > 0) {
        toast.error(`Skipping unresolved SKU(s): ${unresolvedSkus.join(', ')} — not found in Inventory.`);
      }

      const res = await api.post('/inventory/material-receipts', {
        poNumber: po.poNumber,
        supplierName: po.supplierName,
        challanNumber,
        transportName,
        receivedBy,
        items: validItems.map(item => ({
          skuCode: item.skuCode,
          quantity: item.quantity,
          uom: item.uom,
        })),
      });

      toast.success(`${res.data?.data?.mrNumber} recorded at the gate. Form the GRN from Material Receipts when ready.`);
      onSuccess();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to record material receipt');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
        <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-cyan-50 rounded-xl text-cyan-600">
              <Package size={20} />
            </div>
            <div>
              <h2 className="text-lg font-black text-slate-900">Record Material Receipt</h2>
              <p className="text-xs font-bold text-slate-400">Gate entry for PO: {po.poNumber}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Supplier</label>
              <input type="text" disabled value={po.supplierName} className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium text-slate-500" />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Received By</label>
              <input type="text" required value={receivedBy} onChange={e => setReceivedBy(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none" />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Challan / Invoice No</label>
              <input type="text" required value={challanNumber} onChange={e => setChallanNumber(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none" />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5">Transport Name</label>
              <input type="text" required value={transportName} onChange={e => setTransportName(e.target.value)} className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-cyan-500 outline-none" />
            </div>
          </div>

          <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
            <h3 className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-3">Items Received</h3>
            <div className="space-y-2">
              {po.items.map((item, idx) => (
                <div key={idx} className="flex items-center justify-between bg-white px-3 py-2 rounded-lg border border-slate-200 shadow-sm">
                  <div>
                    <span className="font-bold text-sm text-slate-900 block">{item.skuCode}</span>
                  </div>
                  <span className="text-xs font-black bg-cyan-50 text-cyan-700 px-2 py-1 rounded-md">
                    {item.quantity} {item.uom}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-4 flex items-center justify-end gap-3">
            <button type="button" onClick={onClose} disabled={submitting} className="px-5 py-2.5 text-sm font-bold text-slate-500 hover:text-slate-700 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={submitting} className="px-5 py-2.5 bg-cyan-600 hover:bg-cyan-700 text-white text-sm font-black rounded-xl transition-all shadow-lg shadow-cyan-200 flex items-center gap-2 disabled:opacity-50">
              {submitting ? <><Loader2 size={16} className="animate-spin" /> Recording...</> : <><CheckCircle2 size={16} /> Record Gate Receipt</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
