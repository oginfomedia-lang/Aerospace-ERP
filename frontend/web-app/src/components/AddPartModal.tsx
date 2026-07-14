import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { X, Package, MapPin, Hash, ChevronRight, CheckCircle, CalendarClock } from 'lucide-react';
import { ApiResponse, Part, Location } from '../types/inventory';
import { Supplier } from '../types/procurement';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

type Step = 'master' | 'stock';

export const AddPartModal = ({ isOpen, onClose, onSuccess }: Props) => {
  const [step, setStep] = useState<Step>('master');
  const [createdPart, setCreatedPart] = useState<Part | null>(null);

  const [masterData, setMasterData] = useState({
    name: '',
    serialNumber: '',
    partType: '',
    criticality: 'MEDIUM',
    reorderLevel: 5,
    minStock: 0,
    maxStock: 0,
    safetyStock: 0,
    leadTimeDays: 0,
    batchRequired: true,
    serialRequired: false,
    expiryTracking: false,
    qcRequired: true,
    shelfLifeDays: 0,
    preferredSupplierId: '' as number | '',
    unitPrice: '' as number | '',
    taxRatePercent: '' as number | '',
    hsnCode: '',
    description: '',
  });

  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);

  useEffect(() => {
    procurementApi.get('/suppliers').then(res => setSuppliers(res.data)).catch(() => {});
    api.get<Location[]>('/inventory/locations', { params: { activeOnly: true } })
      .then(res => setLocations(res.data)).catch(() => {});
  }, []);

  // FIX 1: expiryDate moved here to Step 2 state — it is a batch/stock property,
  // NOT a part master property. Part master has no expiry, a specific received batch does.
  const [stockData, setStockData] = useState({
    batchNumber: '',
    quantity: 0,
    location: '',
    expiryDate: '',
  });

  if (!isOpen) return null;

  const handleReset = () => {
    setStep('master');
    setCreatedPart(null);
    setMasterData({
      name: '', serialNumber: '', partType: '', criticality: 'MEDIUM', reorderLevel: 5,
      minStock: 0, maxStock: 0, safetyStock: 0, leadTimeDays: 0,
      batchRequired: true, serialRequired: false, expiryTracking: false, qcRequired: true,
      shelfLifeDays: 0, preferredSupplierId: '',
      unitPrice: '', taxRatePercent: '', hsnCode: '', description: '',
    });
    setStockData({ batchNumber: '', quantity: 0, location: '', expiryDate: '' });
  };

  const handleClose = () => {
    handleReset();
    onClose();
  };

  // Step 1: POST /inventory — only master fields, no stock data
  const handleCreateMaster = async (e: React.FormEvent) => {
    e.preventDefault();

    if (masterData.batchRequired && masterData.serialRequired) {
      toast.error('ITM-BR-002: Batch and Serial tracking cannot both be enabled.');
      return;
    }
    if (masterData.expiryTracking && !masterData.shelfLifeDays) {
      toast.error('ITM-BR-003: Shelf life (days) is required when expiry tracking is enabled.');
      return;
    }
    // Min/Max of 0 means "not set" — only enforce ITM-BR-006 once the user has set a real limit
    if (masterData.minStock > 0 && masterData.minStock > masterData.reorderLevel) {
      toast.error('ITM-BR-006: Min Stock must be <= Reorder Level.');
      return;
    }
    if (masterData.maxStock > 0 && masterData.reorderLevel > masterData.maxStock) {
      toast.error('ITM-BR-006: Reorder Level must be <= Max Stock.');
      return;
    }

    // Optional planning fields default to 0 in the form but should be omitted (not sent as 0)
    // so the backend treats them as "not set" rather than a literal zero limit.
    const payload = {
      ...masterData,
      minStock: masterData.minStock || undefined,
      maxStock: masterData.maxStock || undefined,
      safetyStock: masterData.safetyStock || undefined,
      leadTimeDays: masterData.leadTimeDays || undefined,
      shelfLifeDays: masterData.shelfLifeDays || undefined,
      preferredSupplierId: masterData.preferredSupplierId || undefined,
      unitPrice: masterData.unitPrice || undefined,
      taxRatePercent: masterData.taxRatePercent || undefined,
      hsnCode: masterData.hsnCode || undefined,
      description: masterData.description || undefined,
    };

    try {
      const res = await toast.promise(
        api.post<ApiResponse<Part>>('/inventory', payload),
        {
          loading: 'Registering material master...',
          success: (r) => `Part "${r.data.data.name}" created. Now add initial stock.`,
          error: (err) => err.response?.data?.message || 'Failed to create part.',
        }
      );
      setCreatedPart(res.data.data);
      setStep('stock');
    } catch {
      // toast.promise handles display
    }
  };

  // Step 2: POST /inventory/{id}/inward-from-grn
  // FIX 2: expiryDate is now appended to URLSearchParams as ISO LocalDateTime
  // Backend @RequestParam LocalDateTime expiryDate reads "2024-12-31T00:00:00"
  // StockBalance.expiryDate gets saved → ExpiryScheduler can then use it
  const handleAddStock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createdPart) return;

    if (!stockData.batchNumber.trim()) {
      toast.error('Batch number is required for aerospace traceability.');
      return;
    }
    if (stockData.quantity <= 0) {
      toast.error('Quantity must be greater than zero.');
      return;
    }

    const params = new URLSearchParams({
      qty: String(stockData.quantity),
      batchNumber: stockData.batchNumber,
      location: stockData.location,
    });

    // FIX 2: Was missing — expiryDate collected but never sent to backend
    // Converts "2024-12-31" (HTML date input) → "2024-12-31T00:00:00" (Java LocalDateTime)
    if (stockData.expiryDate) {
      params.append('expiryDate', `${stockData.expiryDate}T00:00:00`);
    }

    try {
      const res = await toast.promise(
        api.post<ApiResponse<Part>>(`/inventory/${createdPart.id}/inward-from-grn?${params}`),
        {
          loading: 'Receiving stock and generating GRN...',
          success: (r) => {
            const grn = r.data.data.grnNumber ? ` GRN ${r.data.data.grnNumber} generated.` : '';
            return createdPart.qcRequired
              ? `Batch ${stockData.batchNumber} received.${grn} Awaiting QA approval.`
              : `Batch ${stockData.batchNumber} received.${grn} Available immediately (no QC required).`;
          },
          error: (err) => err.response?.data?.message || 'Stock inward failed.',
        }
      );
      onSuccess();
      handleClose();
    } catch {
      // toast.promise handles display
    }
  };

  const handleSkipStock = () => {
    onSuccess();
    handleClose();
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-in fade-in duration-300">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-200 flex flex-col max-h-[90vh]">

        {/* Header */}
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600 rounded-lg text-white"><Package size={20} /></div>
            <div>
              <h2 className="text-xl font-bold text-slate-800 tracking-tight">
                {step === 'master' ? 'Register New Material' : 'Receive Initial Stock'}
              </h2>
              <p className="text-xs text-slate-500 font-medium uppercase tracking-wider">
                {step === 'master' ? 'Step 1 of 2 — Material Master' : `Step 2 of 2 — GRN Inward for: ${createdPart?.name}`}
              </p>
            </div>
          </div>
          <button onClick={handleClose} className="p-2 hover:bg-slate-200 rounded-full transition-colors text-slate-400">
            <X size={20} />
          </button>
        </div>

        {/* Step Indicator */}
        <div className="flex border-b border-slate-100 shrink-0">
          <div className={`flex-1 py-3 text-center text-xs font-black uppercase tracking-widest transition-colors
            ${step === 'master' ? 'bg-blue-600 text-white' : 'bg-emerald-50 text-emerald-600'}`}>
            {step === 'master' ? '1. Part Identity' :
              <span className="flex items-center justify-center gap-1"><CheckCircle size={12} /> Part Created</span>}
          </div>
          <div className={`flex-1 py-3 text-center text-xs font-black uppercase tracking-widest transition-colors
            ${step === 'stock' ? 'bg-blue-600 text-white' : 'bg-slate-50 text-slate-400'}`}>
            2. Stock Inward
          </div>
        </div>

        {/* ── STEP 1: Material Master ── */}
        {step === 'master' && (
          <form onSubmit={handleCreateMaster} className="flex flex-col flex-1 min-h-0">
          <div className="p-6 space-y-5 overflow-y-auto flex-1">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div className="md:col-span-2">
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Part Name *</label>
                <input type="text" placeholder="e.g. Turbine Blade Assembly" required value={masterData.name}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all font-medium"
                  onChange={e => setMasterData({ ...masterData, name: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block flex items-center gap-1.5">
                  <Hash size={12} /> Serial / SKU *
                </label>
                <input type="text" placeholder="Unique SKU Code" required value={masterData.serialNumber}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, serialNumber: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Part Type</label>
                <input type="text" placeholder="RAW_MATERIAL, CONSUMABLE..." value={masterData.partType}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, partType: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Criticality</label>
                <select value={masterData.criticality}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none font-semibold"
                  onChange={e => setMasterData({ ...masterData, criticality: e.target.value })}>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Reorder Point</label>
                <input type="number" value={masterData.reorderLevel} min={0}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, reorderLevel: Number(e.target.value) })} />
              </div>
              {/* No expiry date in Step 1 — expiry belongs to the batch received in Step 2 */}

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Min Stock (ITM-BR-006)</label>
                <input type="number" value={masterData.minStock || ''} min={0} placeholder="0"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, minStock: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Max Stock (ITM-BR-006)</label>
                <input type="number" value={masterData.maxStock || ''} min={0} placeholder="0"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, maxStock: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Safety Stock</label>
                <input type="number" value={masterData.safetyStock || ''} min={0} placeholder="0"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, safetyStock: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Lead Time (days)</label>
                <input type="number" value={masterData.leadTimeDays || ''} min={0} placeholder="0"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, leadTimeDays: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Unit Price (Rate)</label>
                <input type="number" value={masterData.unitPrice} min={0} step="0.01" placeholder="0.00"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, unitPrice: e.target.value ? Number(e.target.value) : '' })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Tax Rate %</label>
                <input type="number" value={masterData.taxRatePercent} min={0} step="0.01" placeholder="e.g. 18"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setMasterData({ ...masterData, taxRatePercent: e.target.value ? Number(e.target.value) : '' })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">HSN Code</label>
                <input type="text" value={masterData.hsnCode} placeholder="e.g. 0501040165"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all font-mono"
                  onChange={e => setMasterData({ ...masterData, hsnCode: e.target.value })} />
              </div>
              <div className="md:col-span-2">
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Description</label>
                <textarea rows={2} value={masterData.description} placeholder="Short description shown when adding this part to a Work Order"
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all resize-none"
                  onChange={e => setMasterData({ ...masterData, description: e.target.value })} />
              </div>
              <div className="md:col-span-2">
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Preferred Supplier</label>
                <select value={masterData.preferredSupplierId}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none font-semibold"
                  onChange={e => setMasterData({ ...masterData, preferredSupplierId: e.target.value ? Number(e.target.value) : '' })}>
                  <option value="">None — auto-shortage PO uses the first certified supplier</option>
                  {suppliers.map(s => (
                    <option key={s.id} value={s.id}>{s.name} ({s.vendorCode})</option>
                  ))}
                </select>
                <p className="text-[10px] text-slate-400 mt-1 font-medium">
                  If a Work Order needs more than what's APPROVED in stock, a shortage PO is auto-raised to this supplier.
                </p>
              </div>
            </div>

            {/* Tracking Flags — ITM-BR-002 (batch/serial mutually exclusive), ITM-BR-003, GRN-FR-003 */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100">
              <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
                <input type="checkbox" checked={masterData.batchRequired}
                  onChange={e => setMasterData({ ...masterData, batchRequired: e.target.checked, serialRequired: e.target.checked ? false : masterData.serialRequired })} />
                Batch Required
              </label>
              <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
                <input type="checkbox" checked={masterData.serialRequired}
                  onChange={e => setMasterData({ ...masterData, serialRequired: e.target.checked, batchRequired: e.target.checked ? false : masterData.batchRequired })} />
                Serial Required
              </label>
              <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
                <input type="checkbox" checked={masterData.qcRequired}
                  onChange={e => setMasterData({ ...masterData, qcRequired: e.target.checked })} />
                QC Required
              </label>
              <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
                <input type="checkbox" checked={masterData.expiryTracking}
                  onChange={e => setMasterData({ ...masterData, expiryTracking: e.target.checked })} />
                Expiry Tracking
              </label>
              {masterData.expiryTracking && (
                <div className="col-span-2 md:col-span-4">
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Shelf Life (days) *</label>
                  <input type="number" value={masterData.shelfLifeDays || ''} min={1} required
                    placeholder="Required when Expiry Tracking is on"
                    className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                    onChange={e => setMasterData({ ...masterData, shelfLifeDays: Number(e.target.value) || 0 })} />
                </div>
              )}
              {!masterData.qcRequired && (
                <p className="col-span-2 md:col-span-4 text-[10px] text-emerald-600 font-medium">
                  QC not required — received stock will post directly to Available (skips PENDING_QA).
                </p>
              )}
            </div>
          </div>
            <div className="flex gap-4 p-6 pt-4 border-t border-slate-100 shrink-0">
              <button type="button" onClick={handleClose}
                className="flex-1 px-4 py-3 border border-slate-200 rounded-xl hover:bg-slate-50 font-bold text-slate-600 transition-all">
                Cancel
              </button>
              <button type="submit"
                className="flex-[2] bg-blue-600 text-white py-3 rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all active:scale-95 flex items-center justify-center gap-2">
                Create & Add Stock <ChevronRight size={16} />
              </button>
            </div>
          </form>
        )}

        {/* ── STEP 2: Stock Inward ── */}
        {step === 'stock' && (
          <form onSubmit={handleAddStock} className="flex flex-col flex-1 min-h-0">
          <div className="p-6 space-y-5 overflow-y-auto flex-1">
            <div className="p-4 bg-blue-50 rounded-xl border border-blue-100 text-xs text-blue-700 font-medium">
              {createdPart?.qcRequired
                ? <>Stock enters <strong>PENDING_QA</strong>. QA must approve before it becomes available for production.</>
                : <>This item does not require QC — stock will post directly to <strong>APPROVED</strong> and is immediately available.</>}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Batch Number *</label>
                <input type="text" placeholder="e.g. BATCH-2024-001" required value={stockData.batchNumber}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all font-mono"
                  onChange={e => setStockData({ ...stockData, batchNumber: e.target.value })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Quantity *</label>
                <input type="number" min={1} required value={stockData.quantity || ''}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setStockData({ ...stockData, quantity: parseInt(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block flex items-center gap-1.5">
                  <MapPin size={12} /> Warehouse Location
                </label>
                <select value={stockData.location}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setStockData({ ...stockData, location: e.target.value })}>
                  <option value="">— No location set —</option>
                  {locations.map(l => (
                    <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                  ))}
                </select>
              </div>
              <div>
                {/* FIX 1: Expiry date is HERE in Step 2 — it belongs to the batch being received */}
                {/* FIX 2: This value now gets sent in URLSearchParams to the backend */}
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block flex items-center gap-1.5">
                  <CalendarClock size={12} /> Batch Expiry Date
                </label>
                <input type="date" value={stockData.expiryDate}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all"
                  onChange={e => setStockData({ ...stockData, expiryDate: e.target.value })} />
                <p className="text-[10px] text-slate-400 mt-1 font-medium">
                  Used by Expiry Scheduler to auto-reject expired batches nightly.
                </p>
              </div>
            </div>
          </div>

            <div className="flex gap-4 p-6 pt-4 border-t border-slate-100 shrink-0">
              <button type="button" onClick={handleSkipStock}
                className="flex-1 px-4 py-3 border border-slate-200 rounded-xl hover:bg-slate-50 font-bold text-slate-500 transition-all text-sm">
                Skip for Now
              </button>
              <button type="submit"
                className="flex-[2] bg-blue-600 text-white py-3 rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all active:scale-95 flex items-center justify-center gap-2">
                Receive Stock <ChevronRight size={16} />
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
