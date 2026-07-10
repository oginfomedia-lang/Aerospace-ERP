import React, { useState } from 'react';
import { X, PlusCircle, Trash2, Factory, Settings, Users, Calendar, AlertTriangle } from 'lucide-react';
import productionApi from '../api/productionAxios';
import { ProductionPriority, ProductionOrder, ApiResponse } from '../types/production';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  // When set, edits this order's plan instead of creating a new one. The update endpoint
  // doesn't touch the Bill of Materials, so that section is hidden in edit mode.
  editingOrder?: ProductionOrder | null;
}

const PRIORITIES: ProductionPriority[] = [
  ProductionPriority.LOW,
  ProductionPriority.MEDIUM,
  ProductionPriority.HIGH,
  ProductionPriority.URGENT
];

const priorityStyle: Record<ProductionPriority, string> = {
  [ProductionPriority.LOW]:    'border-emerald-300 bg-emerald-50 text-emerald-700',
  [ProductionPriority.MEDIUM]: 'border-amber-300  bg-amber-50  text-amber-700',
  [ProductionPriority.HIGH]:   'border-orange-300 bg-orange-50 text-orange-700',
  [ProductionPriority.URGENT]: 'border-red-400    bg-red-50    text-red-700',
};

interface MaterialInput {
  partId: number;
  partName: string;
  quantityRequired: number;
}

export const CreateProductionOrderModal: React.FC<Props> = ({ isOpen, onClose, onSuccess, editingOrder }) => {
  const isEditMode = !!editingOrder;
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    productName: '',
    productCode: '',
    aircraftType: '',
    plannedQuantity: 1,
    priority: ProductionPriority.MEDIUM,
    assignedTeam: '',
    supervisorName: '',
    plannedStartDate: '',
    plannedEndDate: '',
    remarks: '',
    materials: [] as MaterialInput[],
  });

  const set = (field: string, value: any) =>
    setForm(f => ({ ...f, [field]: value }));

  React.useEffect(() => {
    if (isOpen && editingOrder) {
      setForm({
        productName: editingOrder.productName,
        productCode: editingOrder.productCode || '',
        aircraftType: editingOrder.aircraftType || '',
        plannedQuantity: editingOrder.plannedQuantity,
        priority: editingOrder.priority,
        assignedTeam: editingOrder.assignedTeam || '',
        supervisorName: editingOrder.supervisorName || '',
        plannedStartDate: editingOrder.plannedStartDate ? new Date(editingOrder.plannedStartDate).toISOString().slice(0, 16) : '',
        plannedEndDate: editingOrder.plannedEndDate ? new Date(editingOrder.plannedEndDate).toISOString().slice(0, 16) : '',
        remarks: editingOrder.remarks || '',
        materials: [],
      });
    }
  }, [isOpen, editingOrder]);

  const addMaterial = () =>
    setForm(f => ({ ...f, materials: [...f.materials, { partId: 0, partName: '', quantityRequired: 1 }] }));

  const removeMaterial = (i: number) =>
    setForm(f => ({ ...f, materials: f.materials.filter((_, idx) => idx !== i) }));

  const updateMaterial = (i: number, field: keyof MaterialInput, value: any) =>
    setForm(f => ({
      ...f,
      materials: f.materials.map((m, idx) =>
        idx === i ? { ...m, [field]: value } : m
      ),
    }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.productName.trim() || !form.plannedQuantity) {
      toast.error('Product Name and Planned Quantity are required.');
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        ...form,
        plannedStartDate: form.plannedStartDate ? new Date(form.plannedStartDate).toISOString() : undefined,
        plannedEndDate: form.plannedEndDate ? new Date(form.plannedEndDate).toISOString() : undefined,
        materials: form.materials.filter(m => m.partId > 0)
      };

      if (isEditMode && editingOrder) {
        await toast.promise(
          productionApi.put<ApiResponse<ProductionOrder>>(`/production/${editingOrder.id}`, payload),
          {
            loading: 'Updating production order...',
            success: (res) => `Order ${res.data.data.poNumber} updated successfully`,
            error: (err) => err.response?.data?.message || 'Failed to update production order',
          }
        );
      } else {
        await toast.promise(
          productionApi.post<ApiResponse<ProductionOrder>>('/production', payload),
          {
            loading: 'Creating production order...',
            success: (res) => `Order ${res.data.data.poNumber} created successfully`,
            error: (err) => err.response?.data?.message || 'Failed to create production order',
          }
        );
      }
      onSuccess();
      onClose();
      resetForm();
    } catch {}
    finally { setSubmitting(false); }
  };

  const resetForm = () => setForm({
    productName: '', productCode: '', aircraftType: '', plannedQuantity: 1,
    priority: ProductionPriority.MEDIUM, assignedTeam: '', supervisorName: '',
    plannedStartDate: '', plannedEndDate: '', remarks: '', materials: [],
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between px-8 py-6 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
              <Factory size={20} />
            </div>
            <div>
              <h2 className="text-xl font-black text-slate-900 tracking-tight">
                {isEditMode ? `Edit Production Order: ${editingOrder!.poNumber}` : 'New Production Order'}
              </h2>
              <p className="text-xs text-slate-500 font-medium">
                {isEditMode ? 'Plan details — Bill of Materials is managed separately' : 'Plan manufacturing and material requirements'}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-xl transition text-slate-400">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto flex-1 px-8 py-6 space-y-5">

          {/* Product Details */}
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 md:col-span-1">
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Product Name *
              </label>
              <input
                required
                type="text"
                placeholder="e.g. Turbine Blade"
                value={form.productName}
                onChange={e => set('productName', e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>
            <div className="col-span-2 md:col-span-1">
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Product Code
              </label>
              <div className="relative">
                <Settings size={14} className="absolute left-3 top-3 text-slate-400" />
                <input
                  type="text"
                  placeholder="SKU-100-ABC"
                  value={form.productCode}
                  onChange={e => set('productCode', e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Planned Quantity *
              </label>
              <input
                required
                type="number"
                min={1}
                value={form.plannedQuantity}
                onChange={e => set('plannedQuantity', Number(e.target.value))}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Priority *
              </label>
              <div className="grid grid-cols-4 gap-1.5">
                {PRIORITIES.map(p => (
                  <button
                    key={p}
                    type="button"
                    onClick={() => set('priority', p)}
                    className={`py-2 rounded-xl text-[10px] font-black border-2 transition-all ${
                      form.priority === p
                        ? priorityStyle[p] + ' border-current shadow-sm'
                        : 'border-slate-200 text-slate-400 hover:border-slate-300'
                    }`}
                  >
                    {p}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Assigned Team
              </label>
              <div className="relative">
                <Users size={14} className="absolute left-3 top-3 text-slate-400" />
                <input
                  type="text"
                  placeholder="e.g. Team Alpha"
                  value={form.assignedTeam}
                  onChange={e => set('assignedTeam', e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Supervisor
              </label>
              <input
                type="text"
                placeholder="Supervisor name"
                value={form.supervisorName}
                onChange={e => set('supervisorName', e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Planned Start
              </label>
              <div className="relative">
                <Calendar size={14} className="absolute left-3 top-3 text-slate-400" />
                <input
                  type="datetime-local"
                  value={form.plannedStartDate}
                  onChange={e => set('plannedStartDate', e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Planned End
              </label>
              <div className="relative">
                <Calendar size={14} className="absolute left-3 top-3 text-slate-400" />
                <input
                  type="datetime-local"
                  value={form.plannedEndDate}
                  onChange={e => set('plannedEndDate', e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
              Remarks
            </label>
            <textarea
              rows={2}
              placeholder="Any additional instructions..."
              value={form.remarks}
              onChange={e => set('remarks', e.target.value)}
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none resize-none"
            />
          </div>

          {/* Materials Section — create only; the update endpoint doesn't touch the BOM */}
          {!isEditMode && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest">
                Bill of Materials (BOM)
              </label>
              <button
                type="button"
                onClick={addMaterial}
                className="flex items-center gap-1.5 text-xs font-bold text-indigo-600 hover:text-indigo-700 transition"
              >
                <PlusCircle size={14} /> Add Material
              </button>
            </div>

            {form.materials.length === 0 ? (
              <div className="border-2 border-dashed border-slate-200 rounded-2xl p-6 text-center text-slate-400 text-xs font-bold">
                No materials added. You can also add them later.
              </div>
            ) : (
              <div className="space-y-2">
                {form.materials.map((m, i) => (
                  <div key={i} className="flex items-center gap-2 bg-slate-50 p-2 rounded-xl border border-slate-100">
                    <input
                      type="number"
                      placeholder="Part ID"
                      className="w-20 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                      value={m.partId || ''}
                      onChange={e => updateMaterial(i, 'partId', Number(e.target.value))}
                    />
                    <input
                      type="text"
                      placeholder="Material Name"
                      className="flex-1 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                      value={m.partName}
                      onChange={e => updateMaterial(i, 'partName', e.target.value)}
                    />
                    <input
                      type="number"
                      placeholder="Qty"
                      className="w-16 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                      value={m.quantityRequired}
                      onChange={e => updateMaterial(i, 'quantityRequired', Number(e.target.value))}
                    />
                    <button
                      type="button"
                      onClick={() => removeMaterial(i)}
                      className="p-1.5 text-red-400 hover:text-red-600 transition"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          )}

          {form.priority === ProductionPriority.URGENT && (
            <div className="flex items-center gap-2 p-3 bg-red-50 rounded-2xl border border-red-200">
              <AlertTriangle size={14} className="text-red-600 flex-shrink-0" />
              <p className="text-xs text-red-700 font-bold">
                URGENT — This will be flagged for immediate priority on the production floor.
              </p>
            </div>
          )}
        </form>

        {/* Footer */}
        <div className="px-8 py-5 border-t border-slate-100 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-6 py-2.5 text-sm font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-2xl transition"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="px-8 py-2.5 bg-indigo-600 text-white text-sm font-black rounded-2xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-200 disabled:opacity-60"
          >
            {isEditMode ? 'Save Changes' : 'Create Production Order'}
          </button>
        </div>
      </div>
    </div>
  );
};
