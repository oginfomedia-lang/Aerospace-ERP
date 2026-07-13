import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { X, Edit3, ShieldAlert } from 'lucide-react';
import { Part } from '../types/inventory';
import { Supplier } from '../types/procurement';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  part: Part | null;
  onClose: () => void;
  onSuccess: () => void;
}

// Matches backend updatePart() which only accepts master fields:
// name, partType, criticality, reorderLevel, serialNumber
// Stock/batch/location are managed via StockAdjustmentModal

export const EditPartModal = ({ isOpen, part, onClose, onSuccess }: Props) => {
  const [formData, setFormData] = useState({
    name: '',
    partType: '',
    criticality: 'MEDIUM',
    reorderLevel: 0,
    minStock: 0,
    maxStock: 0,
    safetyStock: 0,
    leadTimeDays: 0,
    itemStatus: 'ACTIVE',
    batchRequired: true,
    serialRequired: false,
    expiryTracking: false,
    qcRequired: true,
    shelfLifeDays: 0,
    preferredSupplierId: '' as number | '',
  });

  const [suppliers, setSuppliers] = useState<Supplier[]>([]);

  useEffect(() => {
    procurementApi.get('/suppliers').then(res => setSuppliers(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (part) {
      setFormData({
        name: part.name,
        partType: part.partType || '',
        criticality: part.criticality || 'MEDIUM',
        reorderLevel: part.reorderLevel || 0,
        minStock: part.minStock || 0,
        maxStock: part.maxStock || 0,
        safetyStock: part.safetyStock || 0,
        leadTimeDays: part.leadTimeDays || 0,
        itemStatus: part.itemStatus || 'ACTIVE',
        batchRequired: part.batchRequired,
        serialRequired: part.serialRequired,
        expiryTracking: part.expiryTracking,
        qcRequired: part.qcRequired,
        shelfLifeDays: part.shelfLifeDays || 0,
        preferredSupplierId: part.preferredSupplierId || '',
      });
    }
  }, [part]);

  if (!isOpen || !part) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (formData.batchRequired && formData.serialRequired) {
      toast.error('ITM-BR-002: Batch and Serial tracking cannot both be enabled.');
      return;
    }
    if (formData.expiryTracking && !formData.shelfLifeDays) {
      toast.error('ITM-BR-003: Shelf life (days) is required when expiry tracking is enabled.');
      return;
    }
    if (formData.minStock > 0 && formData.minStock > formData.reorderLevel) {
      toast.error('ITM-BR-006: Min Stock must be <= Reorder Level.');
      return;
    }
    if (formData.maxStock > 0 && formData.reorderLevel > formData.maxStock) {
      toast.error('ITM-BR-006: Reorder Level must be <= Max Stock.');
      return;
    }

    // Only master fields — serialNumber sent to maintain SKU identity
    // Backend: part.setSkuCode(dto.getSerialNumber()) — must not be changed here
    const payload = {
      name: formData.name,
      partType: formData.partType,
      criticality: formData.criticality,
      reorderLevel: formData.reorderLevel,
      serialNumber: part.serialNumber, // preserve existing SKU — never let user change SKU
      minStock: formData.minStock || undefined,
      maxStock: formData.maxStock || undefined,
      safetyStock: formData.safetyStock || undefined,
      leadTimeDays: formData.leadTimeDays || undefined,
      itemStatus: formData.itemStatus,
      batchRequired: formData.batchRequired,
      serialRequired: formData.serialRequired,
      expiryTracking: formData.expiryTracking,
      qcRequired: formData.qcRequired,
      shelfLifeDays: formData.shelfLifeDays || undefined,
      preferredSupplierId: formData.preferredSupplierId || undefined,
    };

    try {
      await toast.promise(
        api.put(`/inventory/${part.id}`, payload),
        {
          loading: 'Updating material master...',
          success: 'Master record updated successfully.',
          error: (err) => err.response?.data?.message || 'Update failed.',
        }
      );
      onSuccess();
      onClose();
    } catch (err) {
      // toast.promise handles display
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-in fade-in zoom-in-95 duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-200">

        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center rounded-t-2xl">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-600 rounded-lg text-white">
              <Edit3 size={18} />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800 tracking-tight">Edit Material Master</h2>
              <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest">
                SKU: {part.serialNumber} · ID: #{part.id}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-black transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Part Name *</label>
            <input
              type="text"
              value={formData.name}
              required
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 outline-none transition-all font-medium"
              onChange={e => setFormData({ ...formData, name: e.target.value })}
            />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Part Type / Category</label>
            <input
              type="text"
              value={formData.partType}
              placeholder="RAW_MATERIAL, CONSUMABLE, SPARE_PART..."
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-500 outline-none transition-all"
              onChange={e => setFormData({ ...formData, partType: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Criticality</label>
              <select
                value={formData.criticality}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, criticality: e.target.value })}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Reorder Point</label>
              <input
                type="number"
                value={formData.reorderLevel}
                min={0}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, reorderLevel: parseInt(e.target.value) || 0 })}
              />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Min Stock</label>
              <input type="number" value={formData.minStock || ''} min={0} placeholder="0"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, minStock: parseInt(e.target.value) || 0 })} />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Max Stock</label>
              <input type="number" value={formData.maxStock || ''} min={0} placeholder="0"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, maxStock: parseInt(e.target.value) || 0 })} />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Safety Stock</label>
              <input type="number" value={formData.safetyStock || ''} min={0} placeholder="0"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, safetyStock: parseInt(e.target.value) || 0 })} />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Lead Time (days)</label>
              <input type="number" value={formData.leadTimeDays || ''} min={0} placeholder="0"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, leadTimeDays: parseInt(e.target.value) || 0 })} />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Item Status</label>
              <select value={formData.itemStatus}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, itemStatus: e.target.value })}>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="DISCONTINUED">DISCONTINUED</option>
              </select>
            </div>
          </div>

          {/* Tracking Flags — ITM-BR-002 (batch/serial mutually exclusive), ITM-BR-003, GRN-FR-003 */}
          <div className="grid grid-cols-2 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-100">
            <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
              <input type="checkbox" checked={formData.batchRequired}
                onChange={e => setFormData({ ...formData, batchRequired: e.target.checked, serialRequired: e.target.checked ? false : formData.serialRequired })} />
              Batch Required
            </label>
            <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
              <input type="checkbox" checked={formData.serialRequired}
                onChange={e => setFormData({ ...formData, serialRequired: e.target.checked, batchRequired: e.target.checked ? false : formData.batchRequired })} />
              Serial Required
            </label>
            <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
              <input type="checkbox" checked={formData.qcRequired}
                onChange={e => setFormData({ ...formData, qcRequired: e.target.checked })} />
              QC Required
            </label>
            <label className="flex items-center gap-2 text-xs font-bold text-slate-600 cursor-pointer">
              <input type="checkbox" checked={formData.expiryTracking}
                onChange={e => setFormData({ ...formData, expiryTracking: e.target.checked })} />
              Expiry Tracking
            </label>
            {formData.expiryTracking && (
              <div className="col-span-2">
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block">Shelf Life (days) *</label>
                <input type="number" value={formData.shelfLifeDays || ''} min={1} required
                  className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                  onChange={e => setFormData({ ...formData, shelfLifeDays: parseInt(e.target.value) || 0 })} />
              </div>
            )}
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Preferred Supplier</label>
            <select value={formData.preferredSupplierId}
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
              onChange={e => setFormData({ ...formData, preferredSupplierId: e.target.value ? Number(e.target.value) : '' })}>
              <option value="">None — auto-shortage PO uses the first certified supplier</option>
              {suppliers.map(s => (
                <option key={s.id} value={s.id}>{s.name} ({s.vendorCode})</option>
              ))}
            </select>
            <p className="text-[10px] text-slate-400 mt-1 font-medium">
              If a Work Order needs more than what's APPROVED in stock, a shortage PO is auto-raised to this supplier.
            </p>
          </div>

          {/* ERP notice — explain why stock fields are not here */}
          <div className="p-4 bg-amber-50 rounded-xl border border-amber-100 flex gap-3">
            <ShieldAlert size={18} className="text-amber-500 mt-0.5 shrink-0" />
            <p className="text-[10px] leading-relaxed text-amber-700 font-medium">
              Physical quantities, batch assignments, and QA statuses are managed via
              <strong> Stock Adjustments</strong> and <strong>QA Decisions</strong> to maintain ledger integrity.
            </p>
          </div>

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="flex-1 py-3 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
              Cancel
            </button>
            <button type="submit" className="flex-[2] bg-slate-900 text-white py-3 rounded-xl font-bold hover:bg-black transition-all active:scale-[0.98] shadow-lg">
              Update Master Record
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
