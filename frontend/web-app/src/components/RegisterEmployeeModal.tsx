import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Plus } from 'lucide-react';
import productionResourceApi from '../api/productionResourceAxios';
import { ApiResponse } from '../types/inventory';
import { Employee, Skill } from '../types/productionResource';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const RegisterEmployeeModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [designation, setDesignation] = useState('');
  const [department, setDepartment] = useState('');
  const [skills, setSkills] = useState<Skill[]>([]);
  const [selectedSkillIds, setSelectedSkillIds] = useState<number[]>([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [addingSkill, setAddingSkill] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      productionResourceApi.get<ApiResponse<Skill[]>>('/resources/skills')
        .then(res => setSkills(res.data.data))
        .catch(() => {});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const reset = () => {
    setName(''); setDesignation(''); setDepartment('');
    setSelectedSkillIds([]); setNewSkillName('');
  };

  const toggleSkill = (id: number) => {
    setSelectedSkillIds(prev => prev.includes(id) ? prev.filter(s => s !== id) : [...prev, id]);
  };

  const addSkill = async () => {
    if (!newSkillName.trim()) return;
    setAddingSkill(true);
    try {
      const res = await productionResourceApi.post<ApiResponse<Skill>>('/resources/skills', { name: newSkillName });
      const created = res.data.data;
      setSkills(prev => [...prev, created]);
      setSelectedSkillIds(prev => [...prev, created.id]);
      setNewSkillName('');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add skill');
    } finally {
      setAddingSkill(false);
    }
  };

  const submit = async () => {
    if (!name.trim()) { toast.error('Name is required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.post<ApiResponse<Employee>>('/resources/employees', {
          name, designation, department, skillIds: selectedSkillIds,
        }),
        { loading: 'Registering employee...', success: 'Employee registered', error: (err) => err.response?.data?.message || 'Failed to register' }
      );
      onSuccess();
      reset();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Register Employee</h2>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Name</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder="Jane Doe"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Designation</label>
            <input value={designation} onChange={e => setDesignation(e.target.value)} placeholder="CNC Operator"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Department</label>
            <input value={department} onChange={e => setDepartment(e.target.value)} placeholder="Machining"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Skills</label>
          <div className="flex flex-wrap gap-2 mb-2 max-h-32 overflow-y-auto">
            {skills.length === 0 ? (
              <span className="text-xs text-slate-400">No skills yet — add one below</span>
            ) : skills.map(s => (
              <button key={s.id} type="button" onClick={() => toggleSkill(s.id)}
                className={`px-3 py-1.5 rounded-full text-xs font-bold border transition ${
                  selectedSkillIds.includes(s.id) ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-500 border-slate-200'
                }`}>
                {s.name}
              </button>
            ))}
          </div>
          <div className="flex gap-2">
            <input value={newSkillName} onChange={e => setNewSkillName(e.target.value)} placeholder="Add a new skill..."
              className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <button type="button" onClick={addSkill} disabled={addingSkill || !newSkillName.trim()}
              className="px-3 py-2 bg-slate-900 text-white rounded-xl disabled:opacity-40">
              <Plus size={16} />
            </button>
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button onClick={() => { onClose(); reset(); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
          <button onClick={submit} disabled={submitting}
            className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
            Register
          </button>
        </div>
      </div>
    </div>
  );
};
