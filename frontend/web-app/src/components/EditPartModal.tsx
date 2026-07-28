import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { X, Edit3, ShieldAlert, Star, Trash2 } from 'lucide-react';
import { Part, ApprovedSupplier } from '../types/inventory';
import { Supplier } from '../types/procurement';
import toast from 'react-hot-toast';
import { useAuth } from '../context/AuthContext';
import { CAN_MANAGE_PROCUREMENT, hasAnyRole } from '../utils/permissions';

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
  const { user } = useAuth();
  const canManageApprovedSuppliers = hasAnyRole(user?.roles, CAN_MANAGE_PROCUREMENT);
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
    unitPrice: '' as number | '',
    taxRatePercent: '' as number | '',
    hsnCode: '',
    description: '',
  });

  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [approvedSuppliers, setApprovedSuppliers] = useState<ApprovedSupplier[]>([]);
  const [addSupplierId, setAddSupplierId] = useState<number | ''>('');

  useEffect(() => {
    procurementApi.get('/suppliers').then(res => setSuppliers(res.data)).catch(() => {});
  }, []);

  const fetchApprovedSuppliers = () => {
    if (!part) return;
    api.get(`/inventory/parts/${part.id}/suppliers`)
      .then(res => setApprovedSuppliers(res.data.data)).catch(() => setApprovedSuppliers([]));
  };

  useEffect(() => {
    fetchApprovedSuppliers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [part]);

  const handleAddApprovedSupplier = async () => {
    if (!part || !addSupplierId) return;
    try {
      await api.post(`/inventory/parts/${part.id}/suppliers`, { supplierId: Number(addSupplierId), preferred: approvedSuppliers.length === 0 });
      setAddSupplierId('');
      fetchApprovedSuppliers();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add approved supplier.');
    }
  };

  const handleRemoveApprovedSupplier = async (supplierId: number) => {
    if (!part) return;
    try {
      await api.delete(`/inventory/parts/${part.id}/suppliers/${supplierId}`);
      fetchApprovedSuppliers();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to remove approved supplier.');
    }
  };

  const handleMarkPreferred = async (supplierId: number) => {
    if (!part) return;
    try {
      await api.post(`/inventory/parts/${part.id}/suppliers`, { supplierId, preferred: true });
      fetchApprovedSuppliers();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to mark preferred supplier.');
    }
  };

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
        unitPrice: part.unitPrice || '',
        taxRatePercent: part.taxRatePercent || '',
        hsnCode: part.hsnCode || '',
        description: part.description || '',
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
      unitPrice: formData.unitPrice || undefined,
      taxRatePercent: formData.taxRatePercent || undefined,
      hsnCode: formData.hsnCode || undefined,
      description: formData.description || undefined,
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
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-200 flex flex-col max-h-[90vh]">

        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center rounded-t-2xl shrink-0">
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

        <form onSubmit={handleSubmit} className="flex flex-col flex-1 min-h-0">
        <div className="p-6 space-y-5 overflow-y-auto flex-1">
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

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Unit Price (Rate)</label>
              <input type="number" value={formData.unitPrice} min={0} step="0.01" placeholder="0.00"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, unitPrice: e.target.value ? Number(e.target.value) : '' })} />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Tax Rate %</label>
              <input type="number" value={formData.taxRatePercent} min={0} step="0.01" placeholder="e.g. 18"
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-bold"
                onChange={e => setFormData({ ...formData, taxRatePercent: e.target.value ? Number(e.target.value) : '' })} />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">HSN Code</label>
            <input type="text" value={formData.hsnCode} placeholder="e.g. 0501040165"
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-mono"
              onChange={e => setFormData({ ...formData, hsnCode: e.target.value })} />
          </div>

          <div>
            <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Description</label>
            <textarea rows={2} value={formData.description} placeholder="Short description shown when adding this part to a Work Order"
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl outline-none focus:border-indigo-500 font-medium resize-none"
              onChange={e => setFormData({ ...formData, description: e.target.value })} />
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

          <div className="p-4 bg-slate-50 rounded-xl border border-slate-100 space-y-3">
            <label className="text-xs font-bold text-slate-500 uppercase block tracking-wider">Approved Suppliers</label>
            {approvedSuppliers.length === 0 ? (
              <p className="text-xs text-slate-400">
                {canManageApprovedSuppliers ? 'No approved suppliers yet — add one below.' : 'No approved suppliers yet.'}
              </p>
            ) : (
              <div className="space-y-1.5">
                {approvedSuppliers.map(as => {
                  const s = suppliers.find(x => x.id === as.supplierId);
                  return (
                    <div key={as.supplierId} className="flex items-center justify-between gap-2 bg-white px-3 py-2 rounded-lg border border-slate-200">
                      <span className="text-sm font-semibold text-slate-700 truncate">
                        {s ? `${s.name} (${s.vendorCode})` : `Supplier #${as.supplierId}`}
                      </span>
                      {canManageApprovedSuppliers && (
                        <div className="flex items-center gap-2 shrink-0">
                          <button type="button" onClick={() => handleMarkPreferred(as.supplierId)}
                            title={as.preferred ? 'Preferred supplier' : 'Mark as preferred'}
                            className={as.preferred ? 'text-amber-500' : 'text-slate-300 hover:text-amber-500'}>
                            <Star size={16} fill={as.preferred ? 'currentColor' : 'none'} />
                          </button>
                          <button type="button" onClick={() => handleRemoveApprovedSupplier(as.supplierId)}
                            title="Remove" className="text-slate-300 hover:text-red-600">
                            <Trash2 size={16} />
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
            {canManageApprovedSuppliers && (
              <div className="flex gap-2">
                <select value={addSupplierId}
                  className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm outline-none focus:border-indigo-500"
                  onChange={e => setAddSupplierId(e.target.value ? Number(e.target.value) : '')}>
                  <option value="">Add an approved supplier...</option>
                  {suppliers.filter(s => !approvedSuppliers.some(as => as.supplierId === s.id)).map(s => (
                    <option key={s.id} value={s.id}>{s.name} ({s.vendorCode})</option>
                  ))}
                </select>
                <button type="button" onClick={handleAddApprovedSupplier} disabled={!addSupplierId}
                  className="px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-bold rounded-lg transition-all disabled:opacity-50">
                  Add
                </button>
              </div>
            )}
          </div>

          {/* ERP notice — explain why stock fields are not here */}
          <div className="p-4 bg-amber-50 rounded-xl border border-amber-100 flex gap-3">
            <ShieldAlert size={18} className="text-amber-500 mt-0.5 shrink-0" />
            <p className="text-[10px] leading-relaxed text-amber-700 font-medium">
              Physical quantities, batch assignments, and QA statuses are managed via
              <strong> Stock Adjustments</strong> and <strong>QA Decisions</strong> to maintain ledger integrity.
            </p>
          </div>

        </div>
          <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0">
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
