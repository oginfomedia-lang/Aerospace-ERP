import React, { useState } from 'react';
import { X, PlusCircle, Trash2, Plane, Wrench, AlertTriangle, Package, CheckSquare, Square } from 'lucide-react';
import workOrderApi from '../api/workOrderAxios';
import api from '../api/axios';
import { CreateWorkOrderRequest, MaterialLineItemRequest, Priority, ApiResponse, WorkOrder, UpdateWorkOrderRequest } from '../types/workOrder';
import toast from 'react-hot-toast';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  // When set, the modal edits this WO's header fields instead of creating a new one.
  // Material line items aren't editable here — the backend's update endpoint only
  // touches header fields; lines are managed via Issue Material / raise PO elsewhere.
  editingWorkOrder?: WorkOrder | null;
}

const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const priorityStyle: Record<Priority, string> = {
  LOW:      'border-emerald-300 bg-emerald-50 text-emerald-700',
  MEDIUM:   'border-amber-300  bg-amber-50  text-amber-700',
  HIGH:     'border-orange-300 bg-orange-50 text-orange-700',
  CRITICAL: 'border-red-400    bg-red-50    text-red-700',
};

const emptyLine = (): MaterialLineItemRequest => ({
  partId: 0,
  partName: '',
  partNumber: '',
  quantityRequired: 1,
  remarks: '',
});

export const CreateWorkOrderModal: React.FC<Props> = ({ isOpen, onClose, onSuccess, editingWorkOrder }) => {
  const isEditMode = !!editingWorkOrder;
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState<CreateWorkOrderRequest>({
    aircraftId: '',
    aircraftType: '',
    taskDescription: '',
    taskCode: '',
    priority: 'MEDIUM',
    assignedTo: '',
    createdBy: '',
    location: '',
    dueDate: '',
    remarks: '',
    materialItems: [],
  });
  const [parts, setParts] = useState<any[]>([]);
  const [showPartSelector, setShowPartSelector] = useState(false);
  const [selectedPartIds, setSelectedPartIds] = useState<number[]>([]);

  React.useEffect(() => {
    if (isOpen) {
      api.get('/inventory').then(res => setParts(res.data.data)).catch(() => {});
    } else {
      setShowPartSelector(false);
      setSelectedPartIds([]);
    }
  }, [isOpen]);

  React.useEffect(() => {
    if (isOpen && editingWorkOrder) {
      setForm({
        aircraftId: editingWorkOrder.aircraftId,
        aircraftType: editingWorkOrder.aircraftType || '',
        taskDescription: editingWorkOrder.taskDescription,
        taskCode: editingWorkOrder.taskCode || '',
        priority: editingWorkOrder.priority,
        assignedTo: editingWorkOrder.assignedTo || '',
        createdBy: editingWorkOrder.createdBy || '',
        location: editingWorkOrder.location || '',
        dueDate: editingWorkOrder.dueDate ? new Date(editingWorkOrder.dueDate).toISOString().slice(0, 16) : '',
        remarks: editingWorkOrder.remarks || '',
        materialItems: [],
      });
    }
  }, [isOpen, editingWorkOrder]);

  const set = (field: keyof CreateWorkOrderRequest, value: any) =>
    setForm(f => ({ ...f, [field]: value }));

  const addLine = () =>
    setForm(f => ({ ...f, materialItems: [...(f.materialItems ?? []), emptyLine()] }));

  const removeLine = (i: number) =>
    setForm(f => ({ ...f, materialItems: f.materialItems?.filter((_, idx) => idx !== i) }));

  const togglePartSelection = (partId: number) => {
    setSelectedPartIds(prev =>
      prev.includes(partId) ? prev.filter(id => id !== partId) : [...prev, partId]
    );
  };

  const handleAddSelectedParts = () => {
    const newItems = selectedPartIds.map(id => {
      const part = parts.find(p => p.id === id);
      return {
        partId: part.id,
        partName: part.name,
        partNumber: part.serialNumber,
        quantityRequired: 1,
        remarks: ''
      };
    });
    setForm(f => ({ ...f, materialItems: [...(f.materialItems ?? []), ...newItems] }));
    setShowPartSelector(false);
    setSelectedPartIds([]);
  };

  const updateLine = (i: number, field: keyof MaterialLineItemRequest, value: any) =>
    setForm(f => ({
      ...f,
      materialItems: f.materialItems?.map((l, idx) =>
        idx === i ? { ...l, [field]: value } : l
      ),
    }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.aircraftId.trim() || !form.taskDescription.trim()) {
      toast.error('Aircraft ID and Task Description are required.');
      return;
    }
    setSubmitting(true);
    try {
      if (isEditMode && editingWorkOrder) {
        const updateReq: UpdateWorkOrderRequest = {
          taskDescription: form.taskDescription,
          taskCode: form.taskCode || undefined,
          priority: form.priority,
          assignedTo: form.assignedTo || undefined,
          location: form.location || undefined,
          dueDate: form.dueDate ? new Date(form.dueDate).toISOString() : undefined,
          remarks: form.remarks || undefined,
        };
        await toast.promise(
          workOrderApi.put<ApiResponse<WorkOrder>>(`/work-orders/${editingWorkOrder.id}`, updateReq),
          {
            loading: 'Updating work order...',
            success: (res) => `${res.data.data.woNumber} updated successfully`,
            error: (err) => err.response?.data?.message || 'Failed to update work order',
          }
        );
      } else {
        await toast.promise(
          workOrderApi.post<ApiResponse<WorkOrder>>('/work-orders', {
            ...form,
            dueDate: form.dueDate ? new Date(form.dueDate).toISOString() : undefined,
            materialItems: form.materialItems?.filter(l => l.partId > 0),
          }),
          {
            loading: 'Creating work order...',
            success: (res) => `${res.data.data.woNumber} created successfully`,
            error: (err) => err.response?.data?.message || 'Failed to create work order',
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
    aircraftId: '', aircraftType: '', taskDescription: '', taskCode: '',
    priority: 'MEDIUM', assignedTo: '', createdBy: '', location: '',
    dueDate: '', remarks: '', materialItems: [],
  });

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="flex items-center justify-between px-8 py-6 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-600 rounded-2xl text-white shadow-lg shadow-blue-200">
              <Wrench size={20} />
            </div>
            <div>
              <h2 className="text-xl font-black text-slate-900 tracking-tight">
                {isEditMode ? `Edit Work Order: ${editingWorkOrder!.woNumber}` : 'New Work Order'}
              </h2>
              <p className="text-xs text-slate-500 font-medium">
                {isEditMode ? 'Task and scheduling details — material lines are managed separately' : 'Fill in task and material requirements'}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-100 rounded-xl transition text-slate-400">
            <X size={20} />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto flex-1 px-8 py-6 space-y-5">

          {/* Aircraft */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Aircraft ID *
              </label>
              <div className="relative">
                <Plane size={14} className="absolute left-3 top-3 text-slate-400" />
                <input
                  required
                  disabled={isEditMode}
                  type="text"
                  placeholder="e.g. VT-AXB"
                  value={form.aircraftId}
                  onChange={e => set('aircraftId', e.target.value)}
                  className="w-full pl-8 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none disabled:opacity-60"
                />
              </div>
              {isEditMode && <p className="text-[10px] text-slate-400 mt-1">Aircraft can't be changed after creation.</p>}
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Aircraft Type
              </label>
              <input
                disabled={isEditMode}
                type="text"
                placeholder="e.g. Boeing 737-800"
                value={form.aircraftType}
                onChange={e => set('aircraftType', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none disabled:opacity-60"
              />
            </div>
          </div>

          {/* Task */}
          <div>
            <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
              Task Description *
            </label>
            <textarea
              required
              rows={2}
              placeholder="Describe the maintenance task..."
              value={form.taskDescription}
              onChange={e => set('taskDescription', e.target.value)}
              className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none resize-none"
            />
          </div>

          {/* Task Code + Priority */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                ATA Task Code
              </label>
              <input
                type="text"
                placeholder="e.g. ATA-32-00"
                value={form.taskCode}
                onChange={e => set('taskCode', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
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
                    {p === 'CRITICAL' ? 'AOG' : p}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Assigned / Created / Location / Due */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Assigned To
              </label>
              <input
                type="text"
                placeholder="Technician / Team"
                value={form.assignedTo}
                onChange={e => set('assignedTo', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Created By
              </label>
              <input
                type="text"
                placeholder="Your name"
                value={form.createdBy}
                onChange={e => set('createdBy', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Location / Hangar
              </label>
              <input
                type="text"
                placeholder="e.g. Hangar 3 - Bay B"
                value={form.location}
                onChange={e => set('location', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
            <div>
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
                Due Date
              </label>
              <input
                type="datetime-local"
                value={form.dueDate}
                onChange={e => set('dueDate', e.target.value)}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
          </div>

          {/* Remarks */}
          <div>
            <label className="text-xs font-black text-slate-500 uppercase tracking-widest block mb-1.5">
              Remarks
            </label>
            <input
              type="text"
              placeholder="Additional notes..."
              value={form.remarks}
              onChange={e => set('remarks', e.target.value)}
              className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>

          {/* Material Line Items — create only; the update endpoint doesn't touch these */}
          {!isEditMode && (
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-black text-slate-500 uppercase tracking-widest">
                Material Requirements
              </label>
              <button
                type="button"
                onClick={() => setShowPartSelector(!showPartSelector)}
                className="flex items-center gap-1.5 text-xs font-bold text-blue-600 hover:text-blue-700 transition bg-blue-50 px-3 py-1.5 rounded-lg"
              >
                <PlusCircle size={14} /> Add Part from Inventory
              </button>
            </div>

            {showPartSelector && (
              <div className="bg-white border border-blue-200 rounded-2xl p-4 mb-4 shadow-sm relative overflow-hidden">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-sm font-black text-slate-900 flex items-center gap-2">
                    <Package size={16} className="text-blue-500" /> Select Parts
                  </h3>
                  <button type="button" onClick={() => setShowPartSelector(false)} className="text-slate-400 hover:text-slate-600">
                    <X size={16} />
                  </button>
                </div>
                <div className="max-h-60 overflow-y-auto space-y-1 mb-4 pr-2">
                  {parts.map(part => {
                    const isSelected = selectedPartIds.includes(part.id);
                    return (
                      <div
                        key={part.id}
                        onClick={() => togglePartSelection(part.id)}
                        className={`flex items-center justify-between p-3 rounded-xl cursor-pointer transition border ${isSelected ? 'bg-blue-50 border-blue-200' : 'bg-slate-50 border-transparent hover:bg-slate-100'}`}
                      >
                        <div className="flex items-center gap-3">
                          {isSelected ? <CheckSquare size={18} className="text-blue-600" /> : <Square size={18} className="text-slate-300" />}
                          <div>
                            <div className="text-sm font-bold text-slate-900">{part.name}</div>
                            <div className="text-xs text-slate-500">SKU: {part.serialNumber} | Stock: {part.quantity} APPROVED</div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                  {parts.length === 0 && <div className="text-center py-4 text-xs font-bold text-slate-400">Loading inventory or no parts found...</div>}
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={handleAddSelectedParts}
                    disabled={selectedPartIds.length === 0}
                    className="px-5 py-2 bg-blue-600 text-white text-xs font-black rounded-xl hover:bg-blue-700 transition disabled:opacity-50"
                  >
                    Add {selectedPartIds.length} Selected Parts
                  </button>
                </div>
              </div>
            )}

            {(form.materialItems ?? []).length === 0 ? (
              <div className="border-2 border-dashed border-slate-200 rounded-2xl p-6 text-center text-slate-400 text-xs font-bold">
                No parts added yet — click "Add Part from Inventory" to specify material requirements
              </div>
            ) : (
              <div className="space-y-2">
                <div className="grid grid-cols-12 gap-2 px-3 mb-1">
                  <span className="col-span-2 text-[10px] text-slate-400 font-bold">PART ID</span>
                  <span className="col-span-4 text-[10px] text-slate-400 font-bold">NAME</span>
                  <span className="col-span-3 text-[10px] text-slate-400 font-bold">SKU</span>
                  <span className="col-span-2 text-[10px] text-slate-400 font-bold">QTY</span>
                </div>
                {form.materialItems?.map((line, i) => (
                  <div key={i} className="grid grid-cols-12 gap-2 items-center bg-slate-50 rounded-2xl p-3 border border-slate-100">
                    <div className="col-span-2">
                      <input
                        type="number"
                        disabled
                        value={line.partId || ''}
                        className="w-full px-2 py-1.5 bg-slate-100 border border-slate-200 rounded-lg text-xs font-mono text-slate-500"
                      />
                    </div>
                    <div className="col-span-4">
                      <input
                        type="text"
                        disabled
                        value={line.partName}
                        className="w-full px-2 py-1.5 bg-slate-100 border border-slate-200 rounded-lg text-xs text-slate-500"
                      />
                    </div>
                    <div className="col-span-3">
                      <input
                        type="text"
                        disabled
                        value={line.partNumber}
                        className="w-full px-2 py-1.5 bg-slate-100 border border-slate-200 rounded-lg text-xs text-slate-500"
                      />
                    </div>
                    <div className="col-span-2">
                      <input
                        type="number"
                        min={1}
                        placeholder="Qty"
                        value={line.quantityRequired}
                        onChange={e => updateLine(i, 'quantityRequired', Number(e.target.value))}
                        className="w-full px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-blue-500 outline-none"
                      />
                    </div>
                    <div className="col-span-1 flex justify-center">
                      <button
                        type="button"
                        onClick={() => removeLine(i)}
                        className="p-1 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          )}

          {/* AOG Warning */}
          {form.priority === 'CRITICAL' && (
            <div className="flex items-center gap-2 p-3 bg-red-50 rounded-2xl border border-red-200">
              <AlertTriangle size={14} className="text-red-600 flex-shrink-0" />
              <p className="text-xs text-red-700 font-bold">
                AOG — Aircraft On Ground. This work order will be flagged for immediate attention.
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
            className="px-8 py-2.5 bg-blue-600 text-white text-sm font-black rounded-2xl hover:bg-blue-700 transition shadow-lg shadow-blue-200 disabled:opacity-60"
          >
            {isEditMode ? 'Save Changes' : 'Create Work Order'}
          </button>
        </div>
      </div>
    </div>
  );
};
