import React, { useEffect, useState } from 'react';
import { X, Plus, Trash2, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axios';
import productionApi from '../api/productionAxios';
import productionResourceApi from '../api/productionResourceAxios';
import { ApiResponse, RoutingOperation, RoutingTemplate, RoutingTemplateRequest } from '../types/production';
import { Part } from '../types/inventory';
import { Machine, Employee, Skill } from '../types/productionResource';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  template: RoutingTemplate | null; // null = create
}

const emptyOperation = (sequenceNumber: number): RoutingOperation => ({
  sequenceNumber, operationName: '', operationType: 'PRODUCTION', operationCode: '',
  expectedDurationMinutes: undefined, workstation: '', instructions: '',
});

export const RoutingTemplateModal: React.FC<Props> = ({ isOpen, onClose, onSuccess, template }) => {
  const [partNumber, setPartNumber] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [operations, setOperations] = useState<RoutingOperation[]>([emptyOperation(1)]);
  const [saving, setSaving] = useState(false);
  const [parts, setParts] = useState<Part[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  // Transient, per-row "narrow the employee list by this skill" filter — never sent
  // to the backend, only the finally-chosen preferredEmployeeId/Name are.
  const [skillFilterByRow, setSkillFilterByRow] = useState<Record<number, string>>({});

  useEffect(() => {
    if (isOpen) {
      api.get('/inventory').then(res => setParts(res.data.data)).catch(() => {});
      productionResourceApi.get<ApiResponse<Machine[]>>('/resources/machines')
        .then(res => setMachines(res.data.data.filter(m => m.status === 'ACTIVE'))).catch(() => {});
      productionResourceApi.get<ApiResponse<Employee[]>>('/resources/employees')
        .then(res => setEmployees(res.data.data)).catch(() => {});
      productionResourceApi.get<ApiResponse<Skill[]>>('/resources/skills')
        .then(res => setSkills(res.data.data)).catch(() => {});
      setSkillFilterByRow({});
      if (template) {
        setPartNumber(template.partNumber);
        setName(template.name);
        setDescription(template.description || '');
        setIsActive(template.isActive);
        setOperations(template.operations.length ? template.operations : [emptyOperation(1)]);
      } else {
        setPartNumber(''); setName(''); setDescription(''); setIsActive(true);
        setOperations([emptyOperation(1)]);
      }
    }
  }, [isOpen, template]);

  // Picking a part fills in Name/Description for you — remembering exact part numbers
  // by hand is error-prone, and the routing template is naturally "about" that part.
  const handlePartSelect = (skuCode: string) => {
    setPartNumber(skuCode);
    const part = parts.find(p => p.serialNumber === skuCode);
    if (part) {
      setName(part.name);
      setDescription(part.description || '');
    }
  };

  if (!isOpen) return null;

  const updateOp = (idx: number, patch: Partial<RoutingOperation>) => {
    setOperations(prev => prev.map((op, i) => i === idx ? { ...op, ...patch } : op));
  };

  const addOp = () => setOperations(prev => [...prev, emptyOperation(prev.length + 1)]);

  const removeOp = (idx: number) => {
    setOperations(prev => prev.filter((_, i) => i !== idx).map((op, i) => ({ ...op, sequenceNumber: i + 1 })));
  };

  const moveOp = (idx: number, dir: -1 | 1) => {
    setOperations(prev => {
      const next = [...prev];
      const target = idx + dir;
      if (target < 0 || target >= next.length) return prev;
      [next[idx], next[target]] = [next[target], next[idx]];
      return next.map((op, i) => ({ ...op, sequenceNumber: i + 1 }));
    });
  };

  const handleSave = async () => {
    if (!partNumber.trim() || !name.trim()) {
      toast.error('Part number and name are required.');
      return;
    }
    if (operations.some(op => !op.operationName.trim())) {
      toast.error('Every operation needs a name.');
      return;
    }
    const lastOp = operations[operations.length - 1];
    if (lastOp.operationType !== 'QA_INSPECTION') {
      toast.error('The last operation must be a QA Inspection step — nothing can reach final assembly without a quality check after its last production step.');
      return;
    }
    setSaving(true);
    try {
      const payload: RoutingTemplateRequest = { partNumber, name, description, isActive, operations };
      if (template) {
        await productionApi.put<ApiResponse<RoutingTemplate>>(`/production/routing-templates/${template.id}`, payload);
        toast.success(`Routing template "${name}" updated.`);
      } else {
        await productionApi.post<ApiResponse<RoutingTemplate>>('/production/routing-templates', payload);
        toast.success(`Routing template "${name}" created.`);
      }
      onSuccess();
      onClose();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to save routing template.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-200 flex flex-col max-h-[90vh]">
        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
          <h2 className="text-lg font-black text-slate-900">{template ? 'Edit' : 'New'} Routing Template</h2>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Part Number *</label>
              <select value={partNumber} onChange={e => handlePartSelect(e.target.value)}
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">— Select a part —</option>
                {parts.map(p => (
                  <option key={p.id} value={p.serialNumber}>{p.serialNumber} — {p.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Name *</label>
              <input value={name} onChange={e => setName(e.target.value)}
                className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
          </div>
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Description</label>
            <input value={description} onChange={e => setDescription(e.target.value)}
              className="mt-1 w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input type="checkbox" checked={isActive} onChange={e => setIsActive(e.target.checked)} /> Active
          </label>

          <div className="pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-xs font-black text-slate-500 uppercase tracking-wider">Operations (in order)</h3>
              <button onClick={addOp} className="flex items-center gap-1 text-[11px] font-bold text-indigo-600 hover:text-indigo-700">
                <Plus size={14} /> Add Operation
              </button>
            </div>
            <div className="space-y-2">
              {operations.map((op, idx) => {
                const isLast = idx === operations.length - 1;
                const isQa = op.operationType === 'QA_INSPECTION';
                const skillFilter = skillFilterByRow[idx] || '';
                const filteredEmployees = skillFilter
                  ? employees.filter(e => e.skills.some(s => String(s.id) === skillFilter))
                  : employees;
                return (
                  <div key={idx} className={`border rounded-xl p-2 space-y-1.5 ${isQa ? 'bg-indigo-50 border-indigo-200' : 'bg-slate-50 border-slate-200'}`}>
                    <div className="flex items-center gap-2">
                      <div className="flex flex-col text-slate-300">
                        <button onClick={() => moveOp(idx, -1)} disabled={idx === 0} className="hover:text-slate-600 disabled:opacity-30">▲</button>
                        <button onClick={() => moveOp(idx, 1)} disabled={idx === operations.length - 1} className="hover:text-slate-600 disabled:opacity-30">▼</button>
                      </div>
                      <span className="w-6 text-center text-xs font-black text-slate-400">{op.sequenceNumber}</span>
                      <input placeholder="Operation (e.g. Cutting)" value={op.operationName}
                        onChange={e => updateOp(idx, { operationName: e.target.value })}
                        className="flex-1 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                      <select value={op.operationType}
                        onChange={e => updateOp(idx, { operationType: e.target.value as RoutingOperation['operationType'] })}
                        className={`px-2 py-1.5 border rounded-lg text-[11px] font-bold ${isQa ? 'bg-indigo-100 border-indigo-300 text-indigo-700' : 'bg-white border-slate-200 text-slate-600'}`}>
                        <option value="PRODUCTION">Production</option>
                        <option value="QA_INSPECTION">QA Inspection</option>
                      </select>
                      <input placeholder="Code" value={op.operationCode || ''}
                        onChange={e => updateOp(idx, { operationCode: e.target.value })}
                        className="w-20 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                      <input placeholder="Mins" type="number" value={op.expectedDurationMinutes ?? ''}
                        onChange={e => updateOp(idx, { expectedDurationMinutes: e.target.value ? Number(e.target.value) : undefined })}
                        className="w-16 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs" />
                      <button onClick={() => removeOp(idx)} className="text-slate-300 hover:text-red-500">
                        <Trash2 size={14} />
                      </button>
                      {isLast && !isQa && (
                        <span className="text-[9px] font-black text-red-500 uppercase whitespace-nowrap">Must be QA</span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 pl-8">
                      <select value={op.preferredMachineId ?? ''}
                        onChange={e => {
                          const m = machines.find(mm => mm.id === Number(e.target.value));
                          updateOp(idx, { preferredMachineId: m?.id ?? null, preferredMachineCode: m?.machineCode ?? null });
                        }}
                        className="flex-1 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-[11px] text-slate-600">
                        <option value="">— Preferred machine —</option>
                        {machines.map(m => <option key={m.id} value={m.id}>{m.machineCode} — {m.name}</option>)}
                      </select>
                      <select value={skillFilter}
                        onChange={e => setSkillFilterByRow(prev => ({ ...prev, [idx]: e.target.value }))}
                        className="w-36 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-[11px] text-slate-600">
                        <option value="">— Filter by skill —</option>
                        {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                      </select>
                      <select value={op.preferredEmployeeId ?? ''}
                        onChange={e => {
                          const emp = employees.find(ee => ee.id === Number(e.target.value));
                          updateOp(idx, { preferredEmployeeId: emp?.id ?? null, preferredEmployeeName: emp?.name ?? null });
                        }}
                        className="flex-1 px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-[11px] text-slate-600">
                        <option value="">— Preferred employee —</option>
                        {filteredEmployees.map(e => <option key={e.id} value={e.id}>{e.name}{e.designation ? ` (${e.designation})` : ''}</option>)}
                      </select>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0">
          <button onClick={onClose} className="flex-1 py-2.5 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
            Cancel
          </button>
          <button onClick={handleSave} disabled={saving}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl transition-all disabled:opacity-60">
            {saving ? <Loader2 size={16} className="animate-spin" /> : null} {template ? 'Save Changes' : 'Create Template'}
          </button>
        </div>
      </div>
    </div>
  );
};
