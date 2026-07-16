import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { X, SlidersHorizontal, ArrowDownToLine, ShieldCheck, Bookmark, ArrowLeftRight } from 'lucide-react';
import { ApiResponse, Part, Location } from '../types/inventory';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  part: Part | null;
  onClose: () => void;
  onSuccess: () => void;
}

// Maps to backend endpoints:
// INWARD      → POST /inventory/{id}/inward-from-grn
// QA_DECISION → POST /inventory/{id}/qa-decision
// RESERVE     → POST /inventory/{id}/reserve  (you'll need to add this endpoint)
// TRANSFER    → POST /inventory/{id}/transfer  (you'll need to add this endpoint)

type Operation = 'INWARD' | 'QA_DECISION' | 'RESERVE' | 'TRANSFER';

export const StockAdjustmentModal = ({ isOpen, part, onClose, onSuccess }: Props) => {
  const [operation, setOperation] = useState<Operation>('INWARD');

  // Inward fields (F-36 GRN form)
  const [inwardData, setInwardData] = useState({
    qty: 0,
    location: '',
    grnNumber: '',
    challanNumber: '',
    supplierName: '',
    transportName: '',
    invoiceQty: 0,
    inspectionQty: 0,
    inwardRemarks: '',
    expiryDate: '',
  });

  // QA Decision fields
  const [qaData, setQaData] = useState({ batchNumber: part?.batchNumber || '', status: 'APPROVED', remarks: '' });

  // Reserve fields
  const [reserveData, setReserveData] = useState({ batchNumber: part?.batchNumber || '', qty: 0 });

  // Transfer fields
  const [transferData, setTransferData] = useState({ batchNumber: part?.batchNumber || '', fromBin: part?.location || '', toBin: '', qty: 0 });

  const [locations, setLocations] = useState<Location[]>([]);
  useEffect(() => {
    api.get<Location[]>('/inventory/locations', { params: { activeOnly: true } })
      .then(res => setLocations(res.data)).catch(() => {});
  }, []);

  if (!isOpen || !part) return null;

  const handleClose = () => {
    setOperation('INWARD');
    setInwardData({ qty: 0, location: '', grnNumber: '', challanNumber: '',
      supplierName: '', transportName: '', invoiceQty: 0, inspectionQty: 0, inwardRemarks: '', expiryDate: '' });
    setQaData({ batchNumber: part?.batchNumber || '', status: 'APPROVED', remarks: '' });
    setReserveData({ batchNumber: part?.batchNumber || '', qty: 0 });
    setTransferData({ batchNumber: part?.batchNumber || '', fromBin: part?.location || '', toBin: '', qty: 0 });
    onClose();
  };

  // POST /inventory/{id}/inward-from-grn — Step 1+2: Receive & Prepare GRN
  const handleInward = async (e: React.FormEvent) => {
    e.preventDefault();
    if (inwardData.qty <= 0) { toast.error('Received Quantity must be > 0.'); return; }

    const params = new URLSearchParams({
      qty: String(inwardData.qty),
      location: inwardData.location,
    });
    if (inwardData.grnNumber)     params.append('grnNumber',     inwardData.grnNumber);
    if (inwardData.challanNumber) params.append('challanNumber', inwardData.challanNumber);
    if (inwardData.supplierName)  params.append('supplierName',  inwardData.supplierName);
    if (inwardData.transportName) params.append('transportName', inwardData.transportName);
    if (inwardData.inspectionQty > 0) params.append('inspectionQty', String(inwardData.inspectionQty));
    if (inwardData.expiryDate)    params.append('expiryDate', `${inwardData.expiryDate}T00:00:00`);

    try {
      await toast.promise(
        api.post<ApiResponse<Part>>(`/inventory/${part.id}/inward-from-grn?${params}`),
        {
          loading: 'Submitting GRN...',
          success: (r) => `GRN submitted, batch ${r.data.data.batchNumber} assigned. Awaiting concern dept approval (PENDING_QA).`,
          error: (err) => err.response?.data?.message || 'GRN inward failed.',
        }
      );
      onSuccess();
      handleClose();
    } catch {}
  };

  // POST /inventory/{id}/qa-decision?batchNumber=&status=&remarks=
  const handleQADecision = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!qaData.batchNumber.trim()) { toast.error('Batch number required.'); return; }
    if (!qaData.remarks.trim()) { toast.error('QA remarks are required for traceability.'); return; }

    const params = new URLSearchParams({
      batchNumber: qaData.batchNumber,
      status: qaData.status,
      remarks: qaData.remarks,
    });

    try {
      await toast.promise(
        api.post(`/inventory/${part.id}/qa-decision?${params}`),
        {
          loading: 'Processing QA decision...',
          success: `Batch ${qaData.batchNumber} moved to ${qaData.status}.`,
          error: (err) => err.response?.data?.message || 'QA decision failed.',
        }
      );
      onSuccess();
      handleClose();
    } catch {}
  };

  // POST /inventory/{id}/reserve?batch=&quantity=
  const handleReserve = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reserveData.batchNumber.trim()) { toast.error('Batch number required.'); return; }
    if (reserveData.qty <= 0) { toast.error('Quantity must be > 0.'); return; }

    const params = new URLSearchParams({
      batch: reserveData.batchNumber,
      quantity: String(reserveData.qty),
    });

    try {
      await toast.promise(
        api.post(`/inventory/${part.id}/reserve?${params}`),
        {
          loading: 'Reserving stock for work order...',
          success: `${reserveData.qty} units reserved from batch ${reserveData.batchNumber}.`,
          error: (err) => err.response?.data?.message || 'Reservation failed.',
        }
      );
      onSuccess();
      handleClose();
    } catch {}
  };

  // POST /inventory/{id}/transfer?batch=&fromBin=&toBin=&qty=
  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!transferData.fromBin.trim() || !transferData.toBin.trim()) { toast.error('Both bin locations required.'); return; }
    if (transferData.qty <= 0) { toast.error('Quantity must be > 0.'); return; }

    const params = new URLSearchParams({
      batch: transferData.batchNumber,
      fromBin: transferData.fromBin,
      toBin: transferData.toBin,
      qty: String(transferData.qty),
    });

    try {
      await toast.promise(
        api.post(`/inventory/${part.id}/transfer?${params}`),
        {
          loading: 'Transferring stock between bins...',
          success: `Moved ${transferData.qty} units from ${transferData.fromBin} → ${transferData.toBin}.`,
          error: (err) => err.response?.data?.message || 'Transfer failed.',
        }
      );
      onSuccess();
      handleClose();
    } catch {}
  };

  const ops: { key: Operation; label: string; icon: React.ReactNode; color: string }[] = [
    { key: 'INWARD',      label: 'Receive Stock',  icon: <ArrowDownToLine size={14} />, color: 'emerald' },
    { key: 'QA_DECISION', label: 'QA Decision',    icon: <ShieldCheck size={14} />,     color: 'blue'    },
    { key: 'RESERVE',     label: 'Reserve',        icon: <Bookmark size={14} />,        color: 'violet'  },
    { key: 'TRANSFER',    label: 'Bin Transfer',   icon: <ArrowLeftRight size={14} />,  color: 'orange'  },
  ];

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-200">

        {/* Header */}
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center rounded-t-2xl">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-600 rounded-lg text-white">
              <SlidersHorizontal size={18} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">Stock Adjustment</h2>
              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest">{part.name} · SKU: {part.serialNumber}</p>
            </div>
          </div>
          <button onClick={handleClose} className="text-slate-400 hover:text-black transition-colors"><X size={20} /></button>
        </div>

        {/* Operation Tabs */}
        <div className="grid grid-cols-4 border-b border-slate-100">
          {ops.map(op => (
            <button
              key={op.key}
              onClick={() => setOperation(op.key)}
              className={`py-3 text-[10px] font-black uppercase tracking-wider flex flex-col items-center gap-1 transition-colors ${
                operation === op.key ? 'bg-slate-900 text-white' : 'text-slate-400 hover:bg-slate-50'
              }`}
            >
              {op.icon}
              {op.label}
            </button>
          ))}
        </div>

        <div className="p-6">

          {/* INWARD FORM — Step 1 & 2: Receive material & Prepare GRN */}
          {operation === 'INWARD' && (
            <form onSubmit={handleInward} className="space-y-4">
              {/* Step indicator */}
              <div className="p-3 bg-emerald-50 border border-emerald-100 rounded-xl text-xs text-emerald-700 font-medium space-y-0.5">
                <div className="font-black uppercase tracking-wider text-[10px] text-emerald-600 mb-1">F-36 GRN — Goods Receipt Note</div>
                <div>Step 1: Receive material &amp; verify actual qty with challan qty.</div>
                <div>Step 2: GRN is prepared and offered to concern dept for approval. Stock enters <strong>PENDING_QA</strong>.</div>
              </div>

              {/* Row 1: GRN No + Challan/Invoice No */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">GRN Number</label>
                  <input type="text" placeholder="e.g. GRN-2024-001"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 font-mono text-sm"
                    onChange={e => setInwardData({ ...inwardData, grnNumber: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Challan / Invoice No.</label>
                  <input type="text" placeholder="e.g. INV-5678"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 font-mono text-sm"
                    onChange={e => setInwardData({ ...inwardData, challanNumber: e.target.value })} />
                </div>
              </div>

              {/* Row 2: Supplier + Transport */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Supplier Name</label>
                  <input type="text" placeholder="Supplier / Vendor"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, supplierName: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Transport Name</label>
                  <input type="text" placeholder="Transporter"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, transportName: e.target.value })} />
                </div>
              </div>

              {/* Row 3: Invoice Qty | Received Qty | Inspection (OK) Qty */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Invoice Qty</label>
                  <input type="number" min={0} placeholder="As per challan"
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, invoiceQty: parseInt(e.target.value) || 0 })} />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Received Qty *</label>
                  <input type="number" required min={1}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-emerald-300 rounded-xl outline-none focus:border-emerald-500 text-sm font-bold"
                    onChange={e => setInwardData({ ...inwardData, qty: parseInt(e.target.value) || 0 })} />
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Inspection Qty</label>
                  <input type="number" min={0} placeholder="OK qty"
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-blue-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, inspectionQty: parseInt(e.target.value) || 0 })} />
                </div>
              </div>

              {/* Row 4: Bin Location + Expiry Date */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Store / Bin Location</label>
                  <select
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, location: e.target.value })}>
                    <option value="">— No location set —</option>
                    {locations.map(l => (
                      <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Batch Expiry Date</label>
                  <input type="date"
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-emerald-500 text-sm"
                    onChange={e => setInwardData({ ...inwardData, expiryDate: e.target.value })} />
                </div>
              </div>

              <button type="submit" className="w-full bg-emerald-600 text-white py-3 rounded-xl font-bold hover:bg-emerald-700 transition-all active:scale-95">
                Submit GRN &amp; Receive Stock
              </button>
            </form>
          )}

          {/* QA DECISION FORM */}
          {operation === 'QA_DECISION' && (
            <form onSubmit={handleQADecision} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Batch Number *</label>
                <input type="text" required defaultValue={part.batchNumber || ''}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-blue-500 font-mono"
                  onChange={e => setQaData({ ...qaData, batchNumber: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Decision *</label>
                <select value={qaData.status}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-blue-500 font-bold"
                  onChange={e => setQaData({ ...qaData, status: e.target.value })}>
                  <option value="APPROVED">APPROVED — Pass to stock</option>
                  <option value="REJECTED">REJECTED — Quarantine batch</option>
                </select>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">QA Remarks * (required for traceability)</label>
                <textarea required rows={3} placeholder="Inspection notes, certificate numbers..."
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-blue-500 resize-none"
                  onChange={e => setQaData({ ...qaData, remarks: e.target.value })} />
              </div>
              <button type="submit" className="w-full bg-blue-600 text-white py-3 rounded-xl font-bold hover:bg-blue-700 transition-all active:scale-95">
                Submit QA Decision
              </button>
            </form>
          )}

          {/* RESERVE FORM */}
          {operation === 'RESERVE' && (
            <form onSubmit={handleReserve} className="space-y-4">
              <div className="p-3 bg-violet-50 border border-violet-100 rounded-xl text-xs text-violet-700 font-medium">
                Reserved stock is locked for a Work Order and cannot be issued to other requests.
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Batch Number *</label>
                <input type="text" required defaultValue={part.batchNumber || ''}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-violet-500 font-mono"
                  onChange={e => setReserveData({ ...reserveData, batchNumber: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Quantity to Reserve *</label>
                <input type="number" required min={1}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-violet-500"
                  onChange={e => setReserveData({ ...reserveData, qty: parseInt(e.target.value) || 0 })} />
              </div>
              <button type="submit" className="w-full bg-violet-600 text-white py-3 rounded-xl font-bold hover:bg-violet-700 transition-all active:scale-95">
                Reserve Stock
              </button>
            </form>
          )}

          {/* TRANSFER FORM */}
          {operation === 'TRANSFER' && (
            <form onSubmit={handleTransfer} className="space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Batch Number *</label>
                <input type="text" required defaultValue={part.batchNumber || ''}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-orange-500 font-mono"
                  onChange={e => setTransferData({ ...transferData, batchNumber: e.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">From Bin *</label>
                  <select required defaultValue={part.location || ''}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-orange-500"
                    onChange={e => setTransferData({ ...transferData, fromBin: e.target.value })}>
                    <option value="" disabled>Select a location...</option>
                    {locations.map(l => (
                      <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">To Bin *</label>
                  <select required defaultValue=""
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-orange-500"
                    onChange={e => setTransferData({ ...transferData, toBin: e.target.value })}>
                    <option value="" disabled>Select a location...</option>
                    {locations.map(l => (
                      <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Quantity *</label>
                <input type="number" required min={1}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-orange-500"
                  onChange={e => setTransferData({ ...transferData, qty: parseInt(e.target.value) || 0 })} />
              </div>
              <button type="submit" className="w-full bg-orange-500 text-white py-3 rounded-xl font-bold hover:bg-orange-600 transition-all active:scale-95">
                Transfer Stock
              </button>
            </form>
          )}

        </div>
      </div>
    </div>
  );
};
